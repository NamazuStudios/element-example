package examples;

import dev.getelements.elements.sdk.dao.ApplicationDao;
import dev.getelements.elements.sdk.dao.ProfileDao;
import dev.getelements.elements.sdk.dao.UserDao;
import dev.getelements.elements.sdk.model.application.Application;
import dev.getelements.elements.sdk.model.profile.Profile;
import dev.getelements.elements.sdk.model.user.User;
import util.CoreServiceLocator;

import java.util.Map;

public class ProfileMetadata implements Example {

    private User user;
    private Application application;

    private ApplicationDao applicationDao;
    private ProfileDao profileDao;
    private UserDao userDao;

    private String profileId;

    @Override
    public void run() {

        setup();

        //There are several ways to fetch a profile, but for this example we'll fetch a single
        //profile using the simplest method. Although we could just keep the created Profile object in memory,
        //what's more realistic is that you'll have access to its id, so that's what we'll use here.
        final var profile = profileDao.getActiveProfile(profileId);

        //Here, we increment the gold_stars in the profile and save it back in the db
        final var metadata = profile.getMetadata();
        var stars = (int)metadata.get("gold_stars");

        stars += 100;

        metadata.put("gold_stars", stars);
        profileDao.updateMetadata(profileId, metadata);

        //And you're done!


        //Now, just for this exercise, we'll fetch the profile once more to show a different method
        //and verify that the new value matches. Here we fetch a pagination of Profiles that fit the criteria.
        //Most important to note here is that we fetch profiles belonging to the user with the provided id.
        final int offset = 0;
        final int count = 10;
        final String search = null;
        final Long lowerBoundTimestamp = null;
        final Long upperboundTimestamp = null;

        final var profilePagination = profileDao
                .getActiveProfiles(offset, count, search, user.getId(), lowerBoundTimestamp, upperboundTimestamp);

        final var updatedProfile = profilePagination.getObjects()
                .stream()
                .filter(p -> p.getId().equals(profileId))
                .findFirst()
                .orElseThrow();

        assert (int)updatedProfile.getMetadata().get("gold_stars") == stars;
    }

    @Override
    public void cleanup() {
        userDao.softDeleteUser(user.getId());
        applicationDao.softDeleteApplication(application.getId());
        profileDao.softDeleteProfile(profileId);
    }


    private void setup() {
        userDao = (UserDao)CoreServiceLocator.fetchDao(UserDao.class);
        applicationDao = (ApplicationDao)CoreServiceLocator.fetchDao(ApplicationDao.class);
        profileDao = (ProfileDao)CoreServiceLocator.fetchDao(ProfileDao.class);

        user = createUser();
        application = createApplication();
        profileId = createProfile().getId();
    }

    private User createUser() {

        userDao = (UserDao)CoreServiceLocator.fetchDao(UserDao.class);

        final var user = new User();
        user.setName("john_doe");
        user.setEmail("john.doe@example.com");
        user.setLevel(User.Level.USER);

        return userDao.createUserWithPassword(user, "password");
    }

    private Application createApplication() {

        applicationDao = (ApplicationDao)CoreServiceLocator.fetchDao(ApplicationDao.class);

        final var application = new Application();
        application.setName("ExampleApplication");
        application.setDescription("Example Application Description");

        return applicationDao.createOrUpdateInactiveApplication(application);
    }

    private Profile createProfile() {

        profileDao = (ProfileDao)CoreServiceLocator.fetchDao(ProfileDao.class);

        final var profile = new Profile();
        profile.setDisplayName("Player One");
        profile.setApplication(application);
        profile.setMetadata(Map.of("gold_stars", 0));
        profile.setUser(user);

        return profileDao.createOrReactivateProfile(profile);
    }
}
