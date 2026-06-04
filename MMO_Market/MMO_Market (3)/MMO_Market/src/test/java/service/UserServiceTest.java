package service;

import controller.dto.ProfileResponse;
import controller.dto.UpdateProfileRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private dal.UserRepository userRepository;

    @InjectMocks
    private service.UserService userService;

    @Test
    void getMyProfileReturnsActiveUserProfile() {
        model.User user = createUser();
        when(userRepository.findByIdAndIsDeleteFalse(1L)).thenReturn(Optional.of(user));

        ProfileResponse response = userService.getMyProfile(1L);

        assertEquals(1L, response.getId());
        assertEquals("customer@example.com", response.getEmail());
        assertEquals("Nguyễn Văn A", response.getFullName());
        assertEquals("0901234567", response.getPhone());
        assertEquals(100_000L, response.getBalanceVnd());
    }

    @Test
    void updateMyProfileUpdatesOnlyEditableFields() {
        model.User user = createUser();
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("  Nguyễn Văn B  ");
        request.setPhone("");

        when(userRepository.findByIdAndIsDeleteFalse(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        ProfileResponse response = userService.updateMyProfile(1L, request);

        assertEquals("Nguyễn Văn B", response.getFullName());
        assertNull(response.getPhone());
        assertEquals("{\"role\": \"Customer\"}", response.getRole());
        assertEquals(100_000L, response.getBalanceVnd());
        verify(userRepository).save(user);
    }

    @Test
    void updateMyProfileRejectsInvalidPhone() {
        model.User user = createUser();
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("Nguyễn Văn B");
        request.setPhone("123");

        when(userRepository.findByIdAndIsDeleteFalse(1L)).thenReturn(Optional.of(user));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.updateMyProfile(1L, request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void getMyProfileRejectsMissingOrDeletedUser() {
        when(userRepository.findByIdAndIsDeleteFalse(1L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.getMyProfile(1L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    private model.User createUser() {
        return model.User.builder()
                .id(1L)
                .email("customer@example.com")
                .fullName("Nguyễn Văn A")
                .phone("0901234567")
                .role("{\"role\": \"Customer\"}")
                .shopStatus("Pending")
                .balanceVnd(100_000L)
                .isDelete(false)
                .build();
    }
}