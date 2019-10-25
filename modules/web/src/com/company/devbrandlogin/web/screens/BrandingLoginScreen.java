package com.company.devbrandlogin.web.screens;

import com.haulmont.cuba.core.global.GlobalConfig;
import com.haulmont.cuba.core.global.Resources;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.Screens;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.security.auth.AbstractClientCredentials;
import com.haulmont.cuba.security.auth.Credentials;
import com.haulmont.cuba.security.auth.LoginPasswordCredentials;
import com.haulmont.cuba.security.global.InternalAuthenticationException;
import com.haulmont.cuba.security.global.LoginException;
import com.haulmont.cuba.web.App;
import com.haulmont.cuba.web.Connection;
import com.haulmont.cuba.web.DefaultApp;
import com.haulmont.cuba.web.WebConfig;
import com.haulmont.cuba.web.security.LoginCookies;
import com.vaadin.server.Page;
import com.vaadin.server.StreamResource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Locale;

@UiController("myLogin")
@UiDescriptor("branding-login-screen.xml")
public class BrandingLoginScreen extends Screen {
    private static final Logger log = LoggerFactory.getLogger(BrandingLoginScreen.class);

    @Inject
    protected Resources resources;
    @Inject
    protected WebConfig webConfig;
    @Inject
    protected GlobalConfig globalConfig;
    @Inject
    protected DefaultApp app;
    @Inject
    protected Connection connection;
    @Inject
    protected LoginCookies loginCookies;

    @Inject
    protected Screens screens;
    @Inject
    protected Notifications notifications;
    @Inject
    protected MessageBundle messageBundle;

    @Inject
    protected Image logoImage;
    @Inject
    protected HBoxLayout localesSelectBox;
    @Inject
    protected LookupField<Locale> localesSelect;
    @Inject
    protected CheckBox rememberMeCheckBox;
    @Inject
    protected PasswordField passwordField;
    @Inject
    protected TextField<String> loginField;
    @Inject
    protected CssLayout poweredByBox;

    @Subscribe
    public void onInit(InitEvent event) {
        loginField.focus();

        loadStyles();

        initPoweredByLink();

        initLogoImage();

        initDefaultCredentials();

        initLocales();

        initRememberMe();

        initRememberMeLocalesBox();
    }

    protected void loadStyles() {
        StreamResource resource = new StreamResource(() ->
                resources.getResourceAsStream("/com/company/devbrandlogin/web/screens/login.css"),
                "login.css");

        Page.getCurrent().getStyles().add(resource);
    }

    protected void initPoweredByLink() {
        Component poweredByLink = getWindow().getComponent("poweredByLink");
        if (poweredByLink != null) {
            poweredByLink.setVisible(webConfig.getLoginDialogPoweredByLinkVisible());
        }
    }

    protected void initLogoImage() {
        String loginLogoImagePath = messageBundle.getMessage("loginWindow.logoImage");
        if (StringUtils.isBlank(loginLogoImagePath) || "loginWindow.logoImage".equals(loginLogoImagePath)) {
            logoImage.setVisible(false);
        } else {
            logoImage.setSource(ThemeResource.class).setPath(loginLogoImagePath);
        }
    }

    protected void initDefaultCredentials() {
        String defaultUser = webConfig.getLoginDialogDefaultUser();
        if (!StringUtils.isBlank(defaultUser) && !"<disabled>".equals(defaultUser)) {
            loginField.setValue(defaultUser);
        } else {
            loginField.setValue("");
        }

        String defaultPassw = webConfig.getLoginDialogDefaultPassword();
        if (!StringUtils.isBlank(defaultPassw) && !"<disabled>".equals(defaultPassw)) {
            passwordField.setValue(defaultPassw);
        } else {
            passwordField.setValue("");
        }
    }

    protected void initLocales() {
        localesSelect.setOptionsMap(globalConfig.getAvailableLocales());
        localesSelect.setValue(app.getLocale());

        boolean localeSelectVisible = globalConfig.getLocaleSelectVisible();
        localesSelectBox.setVisible(localeSelectVisible);

        // if old layout is used
        Component localesSelectLabel = getWindow().getComponent("localesSelectLabel");
        if (localesSelectLabel != null) {
            localesSelectLabel.setVisible(localeSelectVisible);
        }

        localesSelect.addValueChangeListener(e -> {
            app.setLocale(e.getValue());

            AuthInfo authInfo = new AuthInfo(loginField.getValue(),
                    passwordField.getValue(),
                    rememberMeCheckBox.getValue());

            String screenId = UiControllerUtils.getScreenContext(this)
                    .getWindowInfo()
                    .getId();

            Screen loginScreen = screens.create(screenId, OpenMode.ROOT);

            if (loginScreen instanceof BrandingLoginScreen) {
                ((BrandingLoginScreen) loginScreen).setAuthInfo(authInfo);
            }

            loginScreen.show();
        });
    }

    protected void initRememberMe() {
        if (!webConfig.getRememberMeEnabled()) {
            rememberMeCheckBox.setValue(false);
            rememberMeCheckBox.setVisible(false);
        }
    }

    protected void initRememberMeLocalesBox() {
        Component rememberLocalesBox = getWindow().getComponent("rememberLocalesBox");
        if (rememberLocalesBox != null) {
            rememberLocalesBox.setVisible(rememberMeCheckBox.isVisible() || localesSelect.isVisible());
        }
    }

    protected void setAuthInfo(AuthInfo authInfo) {
        loginField.setValue(authInfo.getLogin());
        passwordField.setValue(authInfo.getPassword());
        rememberMeCheckBox.setValue(authInfo.getRememberMe());

        localesSelect.focus();
    }

    public void login() {
        doLogin();

        if (Boolean.TRUE.equals(rememberMeCheckBox.getValue())) {
            loginCookies.setRememberMeCookies(loginField.getValue());
        } else {
            loginCookies.resetRememberCookies();
        }
    }

    protected void doLogin() {
        String login = loginField.getValue();
        String password = passwordField.getValue() != null ? passwordField.getValue() : "";

        if (StringUtils.isEmpty(login) || StringUtils.isEmpty(password)) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption(messageBundle.getMessage("loginWindow.emptyLoginOrPassword"))
                    .show();
            return;
        }

        try {
            Locale selectedLocale = localesSelect.getValue();
            app.setLocale(selectedLocale);

            doLogin(new LoginPasswordCredentials(login, password, selectedLocale));

            // locale could be set on the server
            if (connection.getSession() != null) {
                Locale loggedInLocale = connection.getSession().getLocale();

                if (globalConfig.getLocaleSelectVisible()) {
                    app.addCookie(App.COOKIE_LOCALE, loggedInLocale.toLanguageTag());
                }
            }
        } catch (InternalAuthenticationException e) {
            log.error("Internal error during login", e);

            showUnhandledExceptionOnLogin(e);
        } catch (LoginException e) {
            log.info("Login failed: {}", e.toString());

            String message = StringUtils.abbreviate(e.getMessage(), 1000);
            showLoginException(message);
        } catch (Exception e) {
            log.warn("Unable to login", e);

            showUnhandledExceptionOnLogin(e);
        }
    }

    protected void showLoginException(String message) {
        String title = messageBundle.getMessage("loginWindow.loginFailed");

        notifications.create(Notifications.NotificationType.ERROR)
                .withCaption(title)
                .withDescription(message)
                .show();
    }

    protected void doLogin(Credentials credentials) throws LoginException {
        if (credentials instanceof AbstractClientCredentials) {
            ((AbstractClientCredentials) credentials).setOverrideLocale(localesSelect.isVisibleRecursive());
        }
        connection.login(credentials);
    }

    protected void showUnhandledExceptionOnLogin(@SuppressWarnings("unused") Exception e) {
        String title = messageBundle.getMessage("loginWindow.loginFailed");
        String message = messageBundle.getMessage("loginWindow.pleaseContactAdministrator");

        notifications.create(Notifications.NotificationType.ERROR)
                .withCaption(title)
                .withDescription(message)
                .show();
    }

    public static class AuthInfo {

        protected final String login;
        protected final String password;
        protected final Boolean rememberMe;

        public AuthInfo(String login, String password, Boolean rememberMe) {
            this.login = login;
            this.password = password;
            this.rememberMe = rememberMe;
        }

        public String getLogin() {
            return login;
        }

        public String getPassword() {
            return password;
        }

        public Boolean getRememberMe() {
            return rememberMe;
        }
    }
}