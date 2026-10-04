package com.mgrtech.sponti_api.user.internal.application;

import com.mgrtech.sponti_api.DatabaseCleaner;
import com.mgrtech.sponti_api.FakeSmsBoxInitializer;
import com.mgrtech.sponti_api.FixedClockTestConfiguration;
import com.mgrtech.sponti_api.ModuleIntegrationTest;
import com.mgrtech.sponti_api.shared.api.deletion.UserDeletionModule;
import com.mgrtech.sponti_api.shared.api.deletion.UserDeletionTaskStatus;
import com.mgrtech.sponti_api.user.api.command.CreateUserCommand;
import com.mgrtech.sponti_api.user.api.deletion.UserDeletionFacade;
import com.mgrtech.sponti_api.user.api.UserRegistrationFacade;
import com.mgrtech.sponti_api.user.internal.repository.UserDeletionTaskRepository;
import com.mgrtech.sponti_api.user.internal.repository.UserPreferenceRepository;
import com.mgrtech.sponti_api.user.internal.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@ModuleIntegrationTest(mode = ApplicationModuleTest.BootstrapMode.DIRECT_DEPENDENCIES)
@ContextConfiguration(initializers = FakeSmsBoxInitializer.class)
@Import(FixedClockTestConfiguration.class)
class UserDeletionApplicationServiceIntegrationTest {

    @Autowired
    UserRegistrationFacade userRegistrationFacade;

    @Autowired
    UserDeletionFacade userDeletionFacade;

    @Autowired
    UserRepository userRepository;

    @Autowired
    UserPreferenceRepository userPreferenceRepository;

    @Autowired
    UserDeletionTaskRepository deletionTaskRepository;

    @Autowired
    DatabaseCleaner databaseCleaner;

    @BeforeEach
    void cleanDatabase() {
        databaseCleaner.clean();
    }

    @Test
    void requestDeletionCreatesTasksAndBlocksFinalDeletionUntilAllModulesComplete() {
        var userId = createUser();

        userDeletionFacade.requestDeletion(userId);

        var user = userRepository.findById(userId).orElseThrow();
        assertThat(user.getStatusAsString()).isEqualTo("DELETION_REQUESTED");
        assertThat(user.getPhoneNumber()).isEqualTo("+32468009911");
        assertThat(userPreferenceRepository.findByUserId(userId)).isPresent();

        assertThat(deletionTaskRepository.findAll())
                .hasSize(UserDeletionModule.values().length)
                .allSatisfy(task -> assertThat(task.getUser().getId()).isEqualTo(userId));

        assertTaskStatus(userId, UserDeletionModule.USER, UserDeletionTaskStatus.COMPLETED);
        assertTaskStatus(userId, UserDeletionModule.AUTH, UserDeletionTaskStatus.PENDING);
        assertTaskStatus(userId, UserDeletionModule.AVAILABILITY, UserDeletionTaskStatus.PENDING);
    }

    @Test
    void requestDeletionCreatesTasksForEachDeletedUser() {
        var firstUserId = createUser("+32468009911");
        var secondUserId = createUser("+32468009912");

        userDeletionFacade.requestDeletion(firstUserId);
        userDeletionFacade.requestDeletion(secondUserId);

        assertThat(deletionTaskRepository.findAll())
                .hasSize(UserDeletionModule.values().length * 2);
        assertThat(deletionTaskRepository.findAll())
                .filteredOn(task -> task.getUser().getId().equals(firstUserId))
                .hasSize(UserDeletionModule.values().length);
        assertThat(deletionTaskRepository.findAll())
                .filteredOn(task -> task.getUser().getId().equals(secondUserId))
                .hasSize(UserDeletionModule.values().length);
    }

    @Test
    void markTaskFailedPersistsFailureWithoutDeletingUser() {
        var userId = createUser();
        userDeletionFacade.requestDeletion(userId);

        userDeletionFacade.markTaskFailed(userId, UserDeletionModule.AVAILABILITY, "cleanup failed");

        var task = deletionTaskRepository.findByUser_IdAndModule(userId, UserDeletionModule.AVAILABILITY)
                .orElseThrow();
        assertThat(task.getStatus()).isEqualTo(UserDeletionTaskStatus.FAILED);
        assertThat(task.getErrorMessage()).isEqualTo("cleanup failed");
        assertThat(task.getCompletedAt()).isNull();

        var user = userRepository.findById(userId).orElseThrow();
        assertThat(user.getStatusAsString()).isEqualTo("DELETION_REQUESTED");
        assertThat(userPreferenceRepository.findByUserId(userId)).isPresent();
    }

    @Test
    void completingAllTasksAnonymizesUserAndDeletesPreferences() {
        var userId = createUser();
        userDeletionFacade.requestDeletion(userId);

        for (UserDeletionModule module : UserDeletionModule.values()) {
            userDeletionFacade.markTaskCompleted(userId, module);
        }

        var user = userRepository.findById(userId).orElseThrow();
        assertThat(user.getStatusAsString()).isEqualTo("DELETED");
        assertThat(user.getDeletedAt()).isNotNull();
        assertThat(user.getPhoneNumber()).isNull();
        assertThat(user.getDisplayName()).isEqualTo("Deleted user");
        assertThat(user.getPasswordHash()).isEmpty();
        assertThat(user.getTimezone()).isNull();
        assertThat(user.isPhoneNumberVerified()).isFalse();
        assertThat(user.isWhatsAppOptIn()).isFalse();
        assertThat(userPreferenceRepository.findByUserId(userId)).isEmpty();
        assertThat(deletionTaskRepository.allCompletedForUser(userId, UserDeletionTaskStatus.COMPLETED)).isTrue();
    }

    private Long createUser() {
        return createUser("+32468009911");
    }

    private Long createUser(String phoneNumber) {
        return userRegistrationFacade.createUser(new CreateUserCommand(
                "password-hash",
                "John",
                phoneNumber,
                "UTC"
        )).id();
    }

    private void assertTaskStatus(Long userId, UserDeletionModule module, UserDeletionTaskStatus status) {
        assertThat(deletionTaskRepository.findByUser_IdAndModule(userId, module))
                .hasValueSatisfying(task -> assertThat(task.getStatus()).isEqualTo(status));
    }
}
