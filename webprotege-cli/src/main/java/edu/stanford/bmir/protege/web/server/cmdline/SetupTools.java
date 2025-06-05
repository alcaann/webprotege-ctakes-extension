package edu.stanford.bmir.protege.web.server.cmdline;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import edu.stanford.bmir.protege.web.server.access.*;
import edu.stanford.bmir.protege.web.server.app.ApplicationPreferencesStore;
import edu.stanford.bmir.protege.web.server.app.ApplicationSettingsManager;
import edu.stanford.bmir.protege.web.server.color.ColorConverter;
import edu.stanford.bmir.protege.web.server.tag.TagIdConverter;
import edu.stanford.bmir.protege.web.server.collection.CollectionIdConverter;
import edu.stanford.bmir.protege.web.server.form.FormIdConverter;
import edu.stanford.bmir.protege.web.server.persistence.*;
import edu.stanford.bmir.protege.web.server.user.UserRecord;
import edu.stanford.bmir.protege.web.server.user.UserRecordConverter;
import edu.stanford.bmir.protege.web.server.user.UserRecordRepository;
import edu.stanford.bmir.protege.web.shared.access.BuiltInRole;
import edu.stanford.bmir.protege.web.shared.access.RoleId;
import edu.stanford.bmir.protege.web.shared.app.*;
import edu.stanford.bmir.protege.web.shared.auth.*;
import edu.stanford.bmir.protege.web.shared.user.EmailAddress;
import edu.stanford.bmir.protege.web.shared.user.UserId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

import javax.annotation.Nonnull;
import java.io.Console;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static edu.stanford.bmir.protege.web.server.cmdline.WebProtegeCli.getMongoClient;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 23 Mar 2017
 */
public class SetupTools {

    private static final String DB_NAME = "webprotege";

    @Nonnull
    private final UserRecordRepository userRecordRepository;

    @Nonnull
    private final AccessManager accessManager;

    @Nonnull
    private final ApplicationSettingsManager applicationSettingsManager;

    private final Console console; // Allow null for non-interactive environments

    public SetupTools(@Nonnull UserRecordRepository userRecordRepository,
            @Nonnull AccessManager accessManager,
            @Nonnull ApplicationSettingsManager applicationSettingsManager,
            Console console) {
        this.userRecordRepository = checkNotNull(userRecordRepository);
        this.accessManager = checkNotNull(accessManager);
        this.applicationSettingsManager = checkNotNull(applicationSettingsManager);
        this.console = console; // Allow null console for Docker environments
    }

    public void createAdministratorAccount() throws IOException {

        // Hardcoded default admin credentials
        String userName = "admin";
        String emailAddress = "admin@admin.com";
        String password = "1234";

        printf("Creating administrator account with default credentials...\n");
        printf("Username: %s\n", userName);
        printf("Email: %s\n", emailAddress);

        createAdministratorAccount(userName, emailAddress, password);

        accessManager.setAssignedRoles(Subject.forUser(userName),
                ApplicationResource.get(),
                Collections.singleton(BuiltInRole.SYSTEM_ADMIN.getRoleId()));

        // Set default permissions for all users
        setDefaultApplicationPermissions();

        // Configure application settings (email and URL)
        configureApplicationSettings();

        printf("You have successfully set up the administrator account, default permissions, and application settings.\n");

        // Uncomment the following lines to prompt for user input instead of using hardcoded credentials + add non null check for console ontop

        /*
         * console.printf("Please enter a user name for the administrator:\n");
         * String userName = console.readLine();
         * console.printf("Please enter an email address for the administrator:\n");
         * String emailAddress = console.readLine();
         * String password = getPassword(console);
         * createAdministratorAccount(userName, emailAddress, password);
         * 
         * accessManager.setAssignedRoles(Subject.forUser(userName),
         * ApplicationResource.get(),
         * Collections.singleton(BuiltInRole.SYSTEM_ADMIN.getRoleId()));
         * 
         * console.printf("You have successfully set up the administrator account.\n");
         */
    }

    private void createAdministratorAccount(@Nonnull String userName,
            @Nonnull String emailAddress,
            @Nonnull String password) {
        Salt salt = getFreshSalt();
        PasswordDigestAlgorithm digestAlgorithm = new PasswordDigestAlgorithm(new Md5DigestAlgorithmProvider());
        SaltedPasswordDigest digest = digestAlgorithm.getDigestOfSaltedPassword(password, salt);
        userRecordRepository.save(new UserRecord(
                UserId.getUserId(userName),
                userName,
                emailAddress,
                "",
                salt,
                digest));
    }

    private void setDefaultApplicationPermissions() {
        printf("Setting up default application permissions...\n");
        
        // Allow guest users (not signed in) to create accounts
        Set<RoleId> guestRoleIds = new HashSet<>(accessManager.getAssignedRoles(Subject.forGuestUser(),
                ApplicationResource.get()));
        guestRoleIds.add(BuiltInRole.ACCOUNT_CREATOR.getRoleId());
        accessManager.setAssignedRoles(Subject.forGuestUser(),
                ApplicationResource.get(),
                guestRoleIds);
        printf("✓ Enabled account creation for guest users\n");

        // Allow any signed-in user to create projects and upload projects
        Set<RoleId> signedInUserRoleIds = new HashSet<>(accessManager.getAssignedRoles(Subject.forAnySignedInUser(),
                ApplicationResource.get()));
        signedInUserRoleIds.add(BuiltInRole.PROJECT_CREATOR.getRoleId());
        signedInUserRoleIds.add(BuiltInRole.PROJECT_UPLOADER.getRoleId());
        accessManager.setAssignedRoles(Subject.forAnySignedInUser(),
                ApplicationResource.get(),
                signedInUserRoleIds);
        printf("✓ Enabled project creation for all signed-in users\n");
        printf("✓ Enabled project upload for all signed-in users\n");
    }

    private void configureApplicationSettings() {
        printf("Configuring application settings...\n");
        
        // Default application configuration values
        String applicationName = "WebProtégé";
        String systemNotificationEmail = "noreply@webprotege.stanford.edu";
        String applicationScheme = "https";
        String applicationHost = "localhost";
        String applicationPath = "";
        int applicationPort = 443;
        long maxUploadSize = 100 * 1024 * 1024; // 100MB
        
        // Create ApplicationLocation with the URL configuration
        ApplicationLocation applicationLocation = new ApplicationLocation(
                applicationScheme,
                applicationHost, 
                applicationPath,
                applicationPort
        );
        
        // Create ApplicationSettings with default values and configured email/URL
        ApplicationSettings applicationSettings = new ApplicationSettings(
                applicationName,
                new EmailAddress(systemNotificationEmail),
                applicationLocation,
                AccountCreationSetting.ACCOUNT_CREATION_ALLOWED,
                java.util.Collections.emptyList(),
                ProjectCreationSetting.EMPTY_PROJECT_CREATION_ALLOWED,
                java.util.Collections.emptyList(),
                ProjectUploadSetting.PROJECT_UPLOAD_ALLOWED,
                java.util.Collections.emptyList(),
                NotificationEmailsSetting.SEND_NOTIFICATION_EMAILS,
                maxUploadSize
        );
        
        // Apply the application settings
        applicationSettingsManager.setApplicationSettings(applicationSettings);
        
        printf("✓ Application name: %s\n", applicationName);
        printf("✓ System notification email: %s\n", systemNotificationEmail);
        printf("✓ Application URL: %s://%s:%d%s\n", applicationScheme, applicationHost, applicationPort, applicationPath);
        printf("✓ Max upload size: %d MB\n", maxUploadSize / (1024 * 1024));
        printf("✓ Application settings configured successfully\n");
    }

    static Salt getFreshSalt() {
        return new SaltProvider().get();
    }

    // Note: getPassword method removed since we use hardcoded credentials
    // If you need interactive password input, uncomment the method below:
    /*
    private String getPassword(Console console) {
        while (true) {
            console.printf("Please enter a password for the administrator account:\n");
            char[] pwd = console.readPassword();
            console.printf("Please confirm the password:\n");
            char[] confPwd = console.readPassword();
            if (Arrays.equals(pwd, confPwd)) {
                return String.valueOf(pwd);
            } else {
                System.out.println("Passwords do not match.  Please try again.");
            }
        }
    }
    */

    public static void main(String[] args) {
        try {
            Morphia morphia = getMorphia();
            MongoClient mongoClient = getMongoClient();
            Datastore datastore = morphia.createDatastore(mongoClient, DB_NAME);
            MongoDatabase database = mongoClient.getDatabase(DB_NAME);
            UserRecordRepository userRecordRepository = new UserRecordRepository(database, new UserRecordConverter());
            AccessManager accessManager = new AccessManagerImpl(RoleOracleImpl.get(), datastore);
            ApplicationPreferencesStore applicationPreferencesStore = new ApplicationPreferencesStore(datastore);
            ApplicationSettingsManager applicationSettingsManager = new ApplicationSettingsManager(accessManager, applicationPreferencesStore);
            
            // Handle case where System.console() might be null (e.g., in Docker)
            Console console = System.console();
            SetupTools tools = new SetupTools(userRecordRepository,
                    accessManager,
                    applicationSettingsManager,
                    console);
            tools.createAdministratorAccount();
        } catch (IOException e) {
            System.out.printf("An error occurred: %s %s\n", e.getClass().getSimpleName(), e.getMessage());
        }
    }

    private static Morphia getMorphia() {
        return new MorphiaProvider(
                new UserIdConverter(),
                new OWLEntityConverter(new OWLDataFactoryImpl()),
                new ProjectIdConverter(),
                new ThreadIdConverter(),
                new CommentIdConverter(),
                new CollectionIdConverter(), new FormIdConverter(), new TagIdConverter(), new ColorConverter()).get();
    }
    
    // Helper method to handle output when console might be null
    private void printf(String format, Object... args) {
        if (console != null) {
            console.printf(format, args);
        } else {
            System.out.printf(format, args);
        }
    }
}
