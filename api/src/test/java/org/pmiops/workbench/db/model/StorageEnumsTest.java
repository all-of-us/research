package org.pmiops.workbench.db.model;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import com.google.api.client.util.Sets;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;
import org.pmiops.workbench.model.Disability;
import org.pmiops.workbench.model.Domain;

public class StorageEnumsTest {
  private final Object INDICATES_STATIC_METHOD = null;

  // files with the DbStorageEnums pattern as of 1 Feb 2021:
  // DbStorageEnums, DemographicSurveyEnum

  final List<Field> allFields =
      Stream.concat(
              Arrays.stream(DbStorageEnums.class.getDeclaredFields()),
              Arrays.stream(DemographicSurveyEnum.class.getDeclaredFields()))
          .collect(Collectors.toList());

  final List<Method> allMethods =
      Stream.concat(
              Arrays.stream(DbStorageEnums.class.getDeclaredMethods()),
              Arrays.stream(DemographicSurveyEnum.class.getDeclaredMethods()))
          .collect(Collectors.toList());

  @Test
  public void test_duplicateFieldNames() {
    Set<String> seenFieldNames = Sets.newHashSet();
    for (Field field : allFields) {
      String fieldName = field.getName();
      assertThat(fieldName).isNotIn(seenFieldNames);
      seenFieldNames.add(fieldName);
    }
  }

  @Test
  public void test_duplicateMethodNames() {
    Set<String> seenMethodNames = Sets.newHashSet();
    for (Method method : allMethods) {
      String methodName = method.getName();
      // TODO: sort out the Disability mess: it can be Boolean, Short, or Disability
      if (!methodName.equals("disabilityToStorage")) {
        assertThat(methodName).isNotIn(seenMethodNames);
      }
      seenMethodNames.add(methodName);
    }
  }

  final Set<Class> allMappedEnumerationClasses = getEnumerationClasses(allFields);

  // special-case test for domain ID round trip

  @Test
  public void test_domainId() {
    for (Domain domainValue : Domain.values()) {
      String storageValue = DbStorageEnums.domainToDomainId(domainValue);
      assertWithMessage("unmapped enum value: " + domainValue).that(storageValue).isNotNull();
      assertThat(DbStorageEnums.domainIdToDomain(storageValue)).isEqualTo(domainValue);
    }
  }

  // special-case test for Disability Short/Boolean methods

  @Test
  public void test_disability() {
    for (Disability disabilityValue : Disability.values()) {
      Short shortValue = DemographicSurveyEnum.disabilityToStorage(disabilityValue);
      assertWithMessage("unmapped enum value (Disability -> Short): " + disabilityValue)
          .that(shortValue)
          .isNotNull();
      Boolean boolValue = DemographicSurveyEnum.boolyDisabilityFromStorage(shortValue);
      assertWithMessage("unmapped enum value (Short -> Boolean): " + disabilityValue)
          .that(boolValue)
          .isNotNull();
      Short shortValue2 = DemographicSurveyEnum.disabilityToStorage(boolValue);
      assertWithMessage("unmapped enum value (Boolean -> Short): " + disabilityValue)
          .that(shortValue2)
          .isNotNull();
      assertThat(shortValue2).isEqualTo(shortValue);
      Disability roundTrip = DemographicSurveyEnum.disabilityFromStorage(shortValue);
      assertWithMessage("unmapped enum value (Short -> Disability): " + disabilityValue)
          .that(roundTrip)
          .isNotNull();
      assertThat(roundTrip).isEqualTo(disabilityValue);
    }
  }

  @Test
  public void test_noMissingMapEntries() throws InvocationTargetException, IllegalAccessException {
    // e.g. public static Short reviewStatusToStorage(ReviewStatus s)
    final Map<Class, Method> enumClassToStorageMethod =
        allMethods.stream()
            // domainToDomainId is stringly typed - test with test_domainId
            .filter(m -> !m.getName().equals("domainToDomainId"))
            .filter(m -> allMappedEnumerationClasses.contains(firstMethodParameterType(m)))
            .collect(Collectors.toMap(this::firstMethodParameterType, m -> m));

    // e.g. public static ReviewStatus reviewStatusFromStorage(Short s)
    final Map<Type, Method> storageMethodToEnumClass =
        allMethods.stream()
            // domainIdToDomain is stringly typed - test with test_domainId
            .filter(m -> !m.getName().equals("domainIdToDomain"))
            .filter(m -> allMappedEnumerationClasses.contains(m.getReturnType()))
            .collect(Collectors.toMap(Method::getReturnType, m -> m));

    for (final Class enumClass : allMappedEnumerationClasses) {
      for (final Object enumValue : enumClass.getEnumConstants()) {
        final Method enumToStorageMethod = enumClassToStorageMethod.get(enumClass);
        Short shortValue = enumToStorage(enumValue, enumToStorageMethod);
        assertThat(shortValue).named(enumClass.getName() + ":" + enumValue.toString()).isNotNull();
        final Method storageToEnumMethod = storageMethodToEnumClass.get(enumClass);
        assertThat(storageToEnum(shortValue, storageToEnumMethod)).isEqualTo(enumValue);
      }
    }
  }

  private Short enumToStorage(Object enumValue, Method enumToStorageMethod)
      throws InvocationTargetException, IllegalAccessException {
    final Object returnValue = enumToStorageMethod.invoke(INDICATES_STATIC_METHOD, enumValue);

    String invokedMethodDesc = String.format("%s(%s)", enumToStorageMethod.getName(), enumValue);

    assertThat(returnValue).named(invokedMethodDesc).isNotNull();
    assertThat(returnValue).named(invokedMethodDesc).isInstanceOf(Short.class);
    return (Short) returnValue;
  }

  private Object storageToEnum(Short shortValue, Method storageToEnumMethod)
      throws InvocationTargetException, IllegalAccessException {
    return storageToEnumMethod.invoke(INDICATES_STATIC_METHOD, shortValue);
  }

  /**
   * Retrieve all Enum classes in DbStorageEnums
   *
   * <p>our convention for BiMaps in DbStorageEnums is <Enum, Short> so we retrieve the first
   * parameterized type
   *
   * @return
   */
  private Set<Class> getEnumerationClasses(List<Field> fields) {
    return fields.stream()
        .map(
            field -> {
              // gets the specific BiMap type including annotations
              final Type biMapType = field.getAnnotatedType().getType();

              // need a ParameterizedType to access the type arguments
              assertThat(biMapType instanceof ParameterizedType).isTrue();
              final ParameterizedType pType = (ParameterizedType) biMapType;

              // our convention is for the enum type to come first
              final Type enumType = pType.getActualTypeArguments()[0];

              // needed for the isEnum() check
              final Class enumClass = (Class) enumType;
              assertThat(enumClass.isEnum()).isTrue();
              return enumClass;
            })
        .collect(Collectors.toSet());
  }

  // convenience method describing what is retrieved here
  private Class firstMethodParameterType(Method method) {
    return method.getParameterTypes()[0];
  }
}
