package com.company.devbrandlogin.web.screens.login;

import com.haulmont.cuba.core.global.GlobalConfig;
import com.haulmont.cuba.core.global.Resources;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.Screens;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.security.global.InternalAuthenticationException;
import com.haulmont.cuba.security.global.LoginException;
import com.haulmont.cuba.web.DefaultApp;
import com.haulmont.cuba.web.WebConfig;
import com.haulmont.cuba.web.security.LoginCookies;
import com.haulmont.cuba.web.security.LoginScreenAuthDelegate;
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
    protected LoginCookies loginCookies;
    @Inject
    protected LoginScreenAuthDelegate authDelegate;

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
    protected Image countryImage;
    @Inject
    protected LookupField<Locale> localesSelect;
    @Inject
    protected CheckBox rememberMeCheckBox;
    @Inject
    protected PasswordField passwordField;
    @Inject
    protected TextField<String> loginField;

    @Subscribe
    public void onInit(InitEvent event) {
        loginField.focus();

        loadStyles();

        initPoweredByLink();

        initLogoImage();

        initDefaultCredentials();

        initLocales();

        initRememberMe();
    }

    @Subscribe
    protected void onAfterShow(AfterShowEvent event) {
        loginCookies.doRememberMeLogin(localesSelect.isVisibleRecursive());
    }

    @Subscribe("submit")
    public void onSubmit(Action.ActionPerformedEvent event) {
        doLogin();

        if (Boolean.TRUE.equals(rememberMeCheckBox.getValue())) {
            loginCookies.setRememberMeCookies(loginField.getValue());
        } else {
            loginCookies.resetRememberCookies();
        }
    }

    protected void loadStyles() {
        StreamResource resource = new StreamResource(() ->
                resources.getResourceAsStream("com/company/devbrandlogin/web/screens/login/resources/login.css"),
                "login.css");

        Page.getCurrent().getStyles().add(resource);
    }

    protected void initPoweredByLink() {
        Component poweredByLink = getWindow().getComponent("poweredByLink");
        if (poweredByLink != null) {
            poweredByLink.setVisible(webConfig.getLoginDialogPoweredByLinkVisible());
        }
    }

    protected void initLocales() {
        localesSelect.setOptionsMap(globalConfig.getAvailableLocales());
        localesSelect.setValue(app.getLocale());
        countryImage.setSource(ClasspathResource.class)
                .setPath("com/company/devbrandlogin/web/screens/login/resources/locales/"
                        + app.getLocale().toString() + ".svg");

        boolean localeSelectVisible = globalConfig.getLocaleSelectVisible();
        localesSelectBox.setVisible(localeSelectVisible);

        // if old layout is used
        Component localesSelectLabel = getWindow().getComponent("localesSelectLabel");
        if (localesSelectLabel != null) {
            localesSelectLabel.setVisible(localeSelectVisible);
        }

        localesSelect.addValueChangeListener(e -> {
            app.setLocale(e.getValue());

            LoginScreenAuthDelegate.AuthInfo authInfo = new LoginScreenAuthDelegate.AuthInfo(loginField.getValue(),
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

    protected void initLogoImage() {
        String loginLogoImagePath = messageBundle.getMessage("loginWindow.logoImage");
        if (StringUtils.isBlank(loginLogoImagePath) || "loginWindow.logoImage".equals(loginLogoImagePath)) {
            logoImage.setVisible(false);
        } else {
            logoImage.setSource(ThemeResource.class).setPath(loginLogoImagePath);
        }
    }

    protected void initRememberMe() {
        if (!webConfig.getRememberMeEnabled()) {
            rememberMeCheckBox.setValue(false);
            rememberMeCheckBox.setVisible(false);
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

    protected void showUnhandledExceptionOnLogin(@SuppressWarnings("unused") Exception e) {
        String title = messageBundle.getMessage("loginWindow.loginFailed");
        String message = messageBundle.getMessage("loginWindow.pleaseContactAdministrator");

        notifications.create(Notifications.NotificationType.ERROR)
                .withCaption(title)
                .withDescription(message)
                .show();
    }

    protected void showLoginException(String message) {
        String title = messageBundle.getMessage("loginWindow.loginFailed");

        notifications.create(Notifications.NotificationType.ERROR)
                .withCaption(title)
                .withDescription(message)
                .show();
    }

    protected void setAuthInfo(LoginScreenAuthDelegate.AuthInfo authInfo) {
        loginField.setValue(authInfo.getLogin());
        passwordField.setValue(authInfo.getPassword());
        rememberMeCheckBox.setValue(authInfo.getRememberMe());

        localesSelect.focus();
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
            authDelegate.doLogin(login, password, localesSelect.getValue(), localesSelect.isVisibleRecursive());
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
}