package it.gov.pagopa.rtp.sender.telemetry;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import java.util.Objects;
import java.util.Optional;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.List;
import org.springframework.core.annotation.AnnotationUtils;

/**
 * Interceptor for adding OpenTelemetry tracing to reactive MongoDB operations.
 * This class intercepts method calls to ReactiveMongoRepository implementations
 * and creates spans to track MongoDB operations for observability purposes.
 */
public class ReactiveMongoTraceInterceptor implements MethodInterceptor {

  private static final List<String> METHODS_TO_IGNORE = List.of("getMongoTemplate", "getMongoDB");

  private final Tracer tracer;
  private final ReactiveMongoTemplate mongoTemplate;

  /**
   * Constructs a new ReactiveMongoTraceInterceptor.
   *
   * @param tracer        OpenTelemetry tracer for creating spans
   * @param mongoTemplate Template for MongoDB operations
   */
  public ReactiveMongoTraceInterceptor(Tracer tracer, ReactiveMongoTemplate mongoTemplate) {
    this.tracer = tracer;
    this.mongoTemplate = mongoTemplate;
  }

  /**
   * Extracts the repository name from a repository instance.
   * Attempts to find the specific ReactiveMongoRepository interface
   * implementation.
   *
   * @param repository The repository instance
   * @return The simple name of the repository interface
   */
  private String getRepositoryName(Object repository) {
    // Get all interfaces implemented by the proxy
    Class<?>[] interfaces = repository.getClass().getInterfaces();

    // Find the repository interface
    return java.util.Arrays.stream(interfaces)
        .filter(ReactiveMongoRepository.class::isAssignableFrom)
        .findFirst()
        .map(Class::getSimpleName)
        .orElse(repository.getClass().getSimpleName());
  }

  /**
   * Converts a repository name to a collection name by converting from CamelCase
   * to snake_case and removing the "Repository" suffix.
   *
   * @param repositoryName Name of the repository class
   * @return The corresponding MongoDB collection name
   */
  private String getCollectionName(String repositoryName) {
    // Convert from CamelCase to snake_case and remove "Repository" suffix
    return repositoryName
        .replaceAll("Repository$", "")
        .replaceAll("([a-z])([A-Z])", "$1_$2")
        .toLowerCase();
  }

  /**
   * Extracts query details from a method invocation, including both
   * explicit @Query
   * annotations and derived query methods, along with their parameters.
   *
   * @param method The intercepted method
   * @param args   The method arguments
   * @return A string containing the query details
   */
  protected String extractQueryDetails(Method method, Object[] args) {
    StringBuilder queryInfo = new StringBuilder();

    // Check for @Query annotation
    Query queryAnnotation = AnnotationUtils.findAnnotation(method, Query.class);
    if (queryAnnotation != null) {
      queryInfo.append("Query: ").append(queryAnnotation.value());
    } else {
      // For derived query methods, convert method name to query representation
      queryInfo.append("Derived: ").append(method.getName());
    }

    return queryInfo.toString();
  }

  /**
   * Main interceptor method that handles method invocations.
   * Checks for TraceMongo annotation and applies tracing if present.
   *
   * @param invocation The method invocation being intercepted
   * @return The result of the method invocation
   * @throws Throwable if the invocation fails
   */
  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    TraceMongo traceMongo = AnnotationUtils.findAnnotation(invocation.getMethod(), TraceMongo.class);
    if (traceMongo == null) {
      traceMongo = AnnotationUtils.findAnnotation(invocation.getThis().getClass(), TraceMongo.class);
    }
    if (traceMongo != null) {
      // Apply tracing logic
      return tracingLogic(invocation);
    } else {
      // Skip tracing
      return invocation.proceed();
    }
  }


  /**
   * Applies tracing logic to the intercepted MongoDB repository method,
   * delegating to either Mono or Flux tracing logic depending on return type.
   *
   * @param invocation The intercepted method invocation
   * @return The traced result, as a {@link Mono} or {@link Flux}
   * @throws Throwable if the method invocation fails
   */
  @Nullable
  private Object tracingLogic(@NonNull final MethodInvocation invocation) throws Throwable {
    // Check if the target is a ReactiveMongoRepository implementation
    if (!(invocation.getThis() instanceof ReactiveMongoRepository<?, ?>))
      return invocation.proceed();

    final var repository = (ReactiveMongoRepository<?, ?>) invocation.getThis();
    final var method = invocation.getMethod();
    final var methodName = method.getName();

    if (METHODS_TO_IGNORE.contains(methodName))
      return invocation.proceed();

    final var repositoryName = getRepositoryName(repository);
    final var collectionName = getCollectionName(repositoryName);
    final var queryDetails = extractQueryDetails(method, invocation.getArguments());
    final var returnType = invocation.getMethod()
        .getReturnType();

    final var span = tracer.spanBuilder(repositoryName + "." + methodName)
        .setParent(Context.current().with(Span.current()))
        .setSpanKind(SpanKind.CLIENT)
        .setAttribute("db.system", "mongodb")
        .setAttribute("db.operation", methodName)
        .setAttribute("db.mongodb.collection", collectionName)
        .setAttribute("db.statement", queryDetails)
        .startSpan();


    return Optional.of(returnType)
        .filter(type -> type.equals(Mono.class))
        .map(type -> (Object) tracingMonoLogic(invocation, span))
        .orElseGet(() -> tracingLogicFlux(invocation, span));
  }


  /**
   * Implements the core tracing logic for MongoDB operations returning a Mono.
   * Creates and manages OpenTelemetry spans for tracking MongoDB operations,
   * including success and error scenarios.
   *
   * @param invocation The method invocation to be traced
   * @return A Mono containing the result of the operation
   */
  @NonNull
  private Mono<?> tracingMonoLogic(
      @NonNull final MethodInvocation invocation,
      @NonNull final Span span) {

    Objects.requireNonNull(invocation);
    Objects.requireNonNull(span);

    return Mono.deferContextual(ctx -> mongoTemplate.getMongoDatabase()
        .switchIfEmpty(Mono.error(new IllegalStateException("Database not available")))
        .flatMap(database -> {
          span.setAttribute("db.name", database.getName());

          try {
            return ((Mono<?>) invocation.proceed())
                .doOnSubscribe(subscription -> span.addEvent("MongoDB operation started"))
                .doOnSuccess(result -> span.addEvent("MongoDB operation completed successfully"))
                .doOnError(error -> this.enrichSpanOnError(span, error))
                .doFinally(signalType -> span.end());
          } catch (Throwable e) {
            this.enrichSpanOnError(span, e);
            span.end();
            return Mono.error(e);
          }
        }));
  }


  /**
   * Implements the core tracing logic for MongoDB operations returning a Flux.
   * Creates and manages OpenTelemetry spans for tracking MongoDB operations,
   * including success and error scenarios.
   *
   * @param invocation The method invocation to be traced
   * @return A Flux containing the result of the operation
   */
  @NonNull
  private Object tracingLogicFlux(
      @NonNull final MethodInvocation invocation,
      @NonNull final Span span) {

    return Flux.deferContextual(ctx -> mongoTemplate.getMongoDatabase()
        .switchIfEmpty(Mono.error(new IllegalStateException("Database not available")))
        .flatMapMany(Flux::just)
        .flatMap(database -> {
          span.setAttribute("db.name", database.getName());

          try {
            return ((Flux<?>) invocation.proceed())
                .doOnSubscribe(subscription -> span.addEvent("MongoDB operation started"))
                .doOnComplete(() -> span.addEvent("MongoDB operation completed successfully"))
                .doOnError(error -> this.enrichSpanOnError(span, error))
                .doFinally(signalType -> span.end());
          } catch (Throwable e) {
            this.enrichSpanOnError(span, e);
            span.end();
            return Flux.error(e);
          }
        }));
  }


  /**
   * Enriches a span with error information and status.
   *
   * @param span the span to enrich
   * @param error the error to enrich the span with
   */
  private void enrichSpanOnError(
      @NonNull final Span span,
      @NonNull final Throwable error) {

    Objects.requireNonNull(span);
    Objects.requireNonNull(error);

    span.recordException(error);
    span.setStatus(StatusCode.ERROR, "MongoDB operation failed: " + error.getMessage());
  }
}