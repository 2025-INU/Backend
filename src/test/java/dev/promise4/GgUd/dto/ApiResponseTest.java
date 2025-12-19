package dev.promise4.GgUd.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApiResponse 테스트")
class ApiResponseTest {

    @Test
    @DisplayName("데이터를 포함한 성공 응답을 생성한다")
    void createSuccessResponseWithData() {
        // given
        String testData = "test data";

        // when
        ApiResponse<String> response = ApiResponse.success(testData);

        // then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Success");
        assertThat(response.getData()).isEqualTo(testData);
        assertThat(response.getTimestamp()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("메시지와 데이터를 포함한 성공 응답을 생성한다")
    void createSuccessResponseWithMessageAndData() {
        // given
        String message = "Custom success message";
        String testData = "test data";

        // when
        ApiResponse<String> response = ApiResponse.success(message, testData);

        // then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo(message);
        assertThat(response.getData()).isEqualTo(testData);
        assertThat(response.getTimestamp()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("메시지만 포함한 에러 응답을 생성한다")
    void createErrorResponseWithMessage() {
        // given
        String errorMessage = "Error occurred";

        // when
        ApiResponse<Object> response = ApiResponse.error(errorMessage);

        // then
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).isEqualTo(errorMessage);
        assertThat(response.getData()).isNull();
        assertThat(response.getTimestamp()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("메시지와 데이터를 포함한 에러 응답을 생성한다")
    void createErrorResponseWithMessageAndData() {
        // given
        String errorMessage = "Error occurred";
        ErrorResponse errorData = new ErrorResponse("E001", "Error details", LocalDateTime.now());

        // when
        ApiResponse<ErrorResponse> response = ApiResponse.error(errorMessage, errorData);

        // then
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).isEqualTo(errorMessage);
        assertThat(response.getData()).isEqualTo(errorData);
        assertThat(response.getTimestamp()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("ErrorResponse를 포함한 에러 응답을 생성한다")
    void createErrorResponseWithErrorData() {
        // given
        ErrorResponse errorData = new ErrorResponse("E001", "Error details", LocalDateTime.now());

        // when
        ApiResponse<ErrorResponse> response = ApiResponse.error(errorData);

        // then
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).isEqualTo("Error");
        assertThat(response.getData()).isEqualTo(errorData);
        assertThat(response.getTimestamp()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("null 데이터로 성공 응답을 생성한다")
    void createSuccessResponseWithNullData() {
        // when
        ApiResponse<Object> response = ApiResponse.success(null);

        // then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Success");
        assertThat(response.getData()).isNull();
        assertThat(response.getTimestamp()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("복잡한 객체를 데이터로 가진 성공 응답을 생성한다")
    void createSuccessResponseWithComplexObject() {
        // given
        record UserDto(String name, int age) {}
        UserDto user = new UserDto("John", 30);

        // when
        ApiResponse<UserDto> response = ApiResponse.success(user);

        // then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo(user);
        assertThat(response.getData().name()).isEqualTo("John");
        assertThat(response.getData().age()).isEqualTo(30);
    }

    @Test
    @DisplayName("timestamp가 응답 생성 시점을 반영한다")
    void timestampReflectsCreationTime() throws InterruptedException {
        // given
        LocalDateTime beforeCreation = LocalDateTime.now();
        Thread.sleep(10);

        // when
        ApiResponse<String> response = ApiResponse.success("test");
        Thread.sleep(10);
        LocalDateTime afterCreation = LocalDateTime.now();

        // then
        assertThat(response.getTimestamp()).isAfter(beforeCreation);
        assertThat(response.getTimestamp()).isBefore(afterCreation);
    }
}
