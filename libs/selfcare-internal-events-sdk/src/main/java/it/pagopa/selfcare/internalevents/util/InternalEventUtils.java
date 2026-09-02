package it.pagopa.selfcare.internalevents.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.pagopa.selfcare.internalevents.model.InternalEvent;
import it.pagopa.selfcare.internalevents.model.InternalEventSessionKey;
import it.pagopa.selfcare.internalevents.model.InternalEventTopic;

import java.lang.reflect.Field;

public class InternalEventUtils {

  public static final ObjectMapper MAPPER = new ObjectMapper();;

  static {
    MAPPER.registerModule(new JavaTimeModule());
  }

  private InternalEventUtils() {}

  public static <T extends InternalEvent> String getTopicName(Class<T> eventType) {
    final InternalEventTopic annotation = eventType.getAnnotation(InternalEventTopic.class);
    if (annotation == null) {
      throw new IllegalArgumentException(eventType.getName() + " must be annotated with @InternalEventTopic");
    }
    return annotation.value();
  }

  public static <T extends InternalEvent> String getSessionId(T event) {
    for (Field field : event.getClass().getDeclaredFields()) {
      if (field.isAnnotationPresent(InternalEventSessionKey.class)) {
        field.setAccessible(true);
        try {
          Object value = field.get(event);
          if (value != null) {
            return value.toString();
          } else {
            throw new IllegalArgumentException("Field annotated with " + InternalEventSessionKey.class.getSimpleName() + " must not be null");
          }
        } catch (IllegalAccessException e) {
          throw new RuntimeException("Error while accessing field annotated with " + InternalEventSessionKey.class.getSimpleName(), e);
        }
      }
    }
    throw new IllegalArgumentException(event.getClass().getName() + " must have a field annotated with " + InternalEventSessionKey.class.getSimpleName());
  }

}
