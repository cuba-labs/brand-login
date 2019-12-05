package com.company.devbrandlogin.web.screens.login;

import com.haulmont.cuba.gui.Route;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.web.app.login.LoginScreen;
import com.haulmont.cuba.web.gui.screen.ScreenDependencyUtils;
import com.vaadin.ui.Dependency;

import javax.inject.Inject;

@Route(path = "login", root = true)
@UiController("login")
@UiDescriptor("app-login-screen.xml")
public class AppLoginScreen extends LoginScreen {

    @Inject
    protected HBoxLayout bottomPanel;

    @Subscribe
    public void onAppLoginScreenInit(InitEvent event) {
        loadStyles();

        initBottomPanel();
    }

    @Subscribe("submit")
    public void onSubmit(Action.ActionPerformedEvent event) {
        login();
    }

    protected void loadStyles() {
        ScreenDependencyUtils.addScreenDependency(this,
                "vaadin://brand-login-screen/login.css", Dependency.Type.STYLESHEET);
    }

    protected void initBottomPanel() {
        if (!webConfig.getLoginDialogPoweredByLinkVisible() && !globalConfig.getLocaleSelectVisible()) {
            bottomPanel.setVisible(false);
        }
    }
}