package com.cinoteck.application.utils.authentication;

import com.cinoteck.application.views.utils.IdleNotification;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;

/**
 * UI content when the user is not logged in yet.
 */
@Route("")
@PageTitle("Login | APMIS")
//@CssImport("./styles/shared-styles.css")
public class LoginView extends FlexLayout implements BeforeEnterObserver {
	private String intendedRoute;
	/**
	 * 
	 */
	private static final long serialVersionUID = -597195523867494116L;

	private transient ResourceBundle resourceBundle = ResourceBundle.getBundle("MockDataWords",
			UI.getCurrent().getLocale() == Locale.US ? Locale.ROOT : UI.getCurrent().getLocale());

	private AccessControl accessControl;

	public LoginView() {
		accessControl = AccessControlFactory.getInstance().createAccessControl();
		buildUI();
	}

	private void buildUI() {
		setSizeFull();
		setClassName("login-screen");

		LoginForm loginForm = new LoginForm();
		loginForm.setI18n(createLoginI18n());
		loginForm.addLoginListener(this::login);
		loginForm.addForgotPasswordListener(event -> Notification.show(resourceBundle.getString("login_hint")));

		VerticalLayout loginInformation = new VerticalLayout();

		loginInformation.setSizeFull();
		loginInformation.setJustifyContentMode(JustifyContentMode.CENTER);
		loginInformation.setAlignItems(Alignment.CENTER);

		loginInformation.setClassName("login-information");
		Image imgApmis = new Image("images/apmislogo.png", "APMIS-LOGO");
		imgApmis.setClassName("apmis-login-logo");
		loginInformation.add(imgApmis);
		loginInformation.add(loginForm);

//		LanguageSwitcher langSwitch  = new LanguageSwitcher(Locale.ENGLISH, new Locale("fa", "IR", "فارسی"));
//		langSwitch.setId("loginLanguageSwitcher");
//		langSwitch.getStyle().set("color", "white !important");
//		loginInformation.add(langSwitch);

		add(loginInformation);
	}

	private void login(LoginForm.LoginEvent event) {
		if (accessControl.signIn(event.getUsername(), event.getPassword())) {

//			IdleNotification idleNotification = new IdleNotification();
//
//			// No. of secs before timeout, at which point the notification is displayed
//			idleNotification.setSecondsBeforeNotification(40);
//			idleNotification.setMessage("Your session will expire in " +  
//			    IdleNotification.MessageFormatting.SECS_TO_TIMEOUT  
//			    + " seconds.");
//			idleNotification.addExtendSessionButton("Extend session");
//			idleNotification.addRedirectButton("Logout now", "logout");
//			idleNotification.addCloseButton();
//			idleNotification.setExtendSessionOnOutsideClick(false);
//
//			 UI.getCurrent().add(idleNotification);
			
			VaadinSession.getCurrent().getSession().setMaxInactiveInterval ( 
					( int ) TimeUnit.MINUTES.toSeconds( 1 ) 
					);
			
			
			System.out.println("__________________________intended root = _________________________________________: "
					+ intendedRoute);
			if (intendedRoute != null) {
				if (intendedRoute.equals("logout")) {
					getUI().get().navigate("/dashboard");
				} else {
					getUI().get().navigate("/" + intendedRoute);
				}
			}else {
				getUI().get().navigate("/dashboard");
			}
//			UI.getCurrent().getPage().reload();

		} else {
			event.getSource().setError(true);
		}
	}

	private LoginI18n createLoginI18n() {
		final LoginI18n i18n = LoginI18n.createDefault();

		i18n.setHeader(new LoginI18n.Header());
		i18n.getForm().setUsername(resourceBundle.getString("username"));
		i18n.getForm().setTitle(resourceBundle.getString("login"));
		i18n.getForm().setSubmit(resourceBundle.getString("login"));
		i18n.getForm().setPassword(resourceBundle.getString("password"));
		i18n.getForm().setForgotPassword(resourceBundle.getString("forgot_pass"));
		i18n.getErrorMessage().setTitle(resourceBundle.getString("login_error_title"));
		i18n.getErrorMessage().setMessage(resourceBundle.getString("login_error_msg"));
		return i18n;
	}

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		// Store the intended route in the UI instance before navigating to the login
		// page

		UI.getCurrent().getPage().executeJs("return document.location.pathname").then(String.class, pageTitle -> {
			if (pageTitle.contains("flow/")) {
				intendedRoute = pageTitle.split("flow/")[1];
				System.out
						.println("___________________________/////___________________________________________________: "
								+ String.format("Page title: '%s'", pageTitle.split("flow/")[1]));
			}
//			Notification.show(String.format("Page title: '%s'", pageTitle));
		});

//		 VaadinServletRequest request = (VaadinServletRequest) VaadinService.getCurrentRequest();

	}
}
