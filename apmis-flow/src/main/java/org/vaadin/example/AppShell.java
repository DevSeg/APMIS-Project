package org.vaadin.example;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.LoadingIndicatorConfiguration;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.theme.Theme;

/**
 * Use the @PWA annotation make the application installable on phones, tablets
 * and some desktop browsers.
 */
@PWA(name = "APMIS Project", shortName = "APMIS")
@Theme("apmis-theme")
public class AppShell implements AppShellConfigurator{

//    @Override
//    public void serviceInit(ServiceInitEvent serviceInitEvent) {
//        serviceInitEvent.getSource().addUIInitListener(uiInitEvent -> {
//            LoadingIndicatorConfiguration conf = uiInitEvent.getUI().getLoadingIndicatorConfiguration();
//
//            // disable default theme -> loading indicator isn't shown
//            conf.setApplyDefaultTheme(true);
//
//            /*
//             * Delay for showing the indicator and setting the 'first' class name.
//             */
//            conf.setFirstDelay(300); // 300ms is the default
//
//            /* Delay for setting the 'second' class name */
//            conf.setSecondDelay(1500); // 1500ms is the default
//
//            /* Delay for setting the 'third' class name */
//            conf.setThirdDelay(5000); // 5000ms is the default
//        });
//    }
}
