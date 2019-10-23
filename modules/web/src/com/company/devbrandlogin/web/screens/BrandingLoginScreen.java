package com.company.devbrandlogin.web.screens;

import com.haulmont.cuba.core.global.GlobalConfig;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.core.global.Resources;
import com.haulmont.cuba.gui.Screens;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.web.DefaultApp;
import com.haulmont.cuba.web.WebConfig;
import com.vaadin.server.Page;
import com.vaadin.server.StreamResource;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.util.Locale;

@UiController("myLogin")
@UiDescriptor("branding-login-screen.xml")
public class BrandingLoginScreen extends Screen {

    @Inject
    protected Resources resources;
    @Inject
    protected WebConfig webConfig;
    @Inject
    protected GlobalConfig globalConfig;
    @Inject
    protected Messages messages;
    @Inject
    protected DefaultApp app;

    @Inject
    protected Screens screens;

    @Inject
    protected Image logoImage;
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
        loadStyles();

        initPoweredByLink();

        initLogoImage();

        initLocales();
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
        String loginLogoImagePath = messages.getMainMessage("loginWindow.logoImage", app.getLocale());
        if (StringUtils.isBlank(loginLogoImagePath) || "loginWindow.logoImage".equals(loginLogoImagePath)) {
            logoImage.setVisible(false);
        } else {
            logoImage.setSource(ThemeResource.class).setPath(loginLogoImagePath);
        }
    }

    protected void initLocales() {
        localesSelect.setOptionsMap(globalConfig.getAvailableLocales());
        localesSelect.setValue(app.getLocale());

        boolean localeSelectVisible = globalConfig.getLocaleSelectVisible();
        localesSelect.setVisible(localeSelectVisible);

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

    protected void setAuthInfo(AuthInfo authInfo) {
        loginField.setValue(authInfo.getLogin());
        passwordField.setValue(authInfo.getPassword());
        rememberMeCheckBox.setValue(authInfo.getRememberMe());

        localesSelect.focus();
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