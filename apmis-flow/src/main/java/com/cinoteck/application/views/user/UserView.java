package com.cinoteck.application.views.user;
 
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
 
import com.cinoteck.application.views.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.MultiSortPriority;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.infrastructure.area.AreaReferenceDto;
import de.symeda.sormas.api.infrastructure.district.DistrictReferenceDto;
import de.symeda.sormas.api.infrastructure.region.RegionReferenceDto;
import de.symeda.sormas.api.report.CommunityUserReportModelDto;
import de.symeda.sormas.api.user.UserDto;
 
@PageTitle("User Management")
@Route(value = "user", layout = MainLayout.class)
 
public class UserView extends VerticalLayout {
	
	public static final String ACTIVE_FILTER = I18nProperties.getString(Strings.active);
	public static final String INACTIVE_FILTER = I18nProperties.getString(Strings.inactive);
 
	List<AreaReferenceDto> regions = FacadeProvider.getAreaFacade().getAllActiveAsReference();
	List<RegionReferenceDto> provinces = FacadeProvider.getRegionFacade().getAllActiveAsReference();
	List<DistrictReferenceDto> districts = FacadeProvider.getDistrictFacade().getAllActiveAsReference();
 
	Grid<UserDto> grid = new Grid<>(UserDto.class, false);
	private GridListDataView<UserDto> dataView;
	UserForm form;
 
 
	Button createUserButton = new Button("New User");
	Button exportUsersButton = new Button("Export Users");
	Button exportRolesButton = new Button("Export User Roles");
	Button bulkModeButton = new Button("Enter Bulk Mode");
	TextField searchField = new TextField();
 
	private static final String CSV_FILE_PATH = "./result.csv";
//	private UsersFilter userFilter = new UsersFilter();
 
	public UserView() {
		setHeightFull();
//		add(userFilter);
		addFilters();
		configureGrid();
		configureForm();
		add(getContent());
		closeEditor();
	}
 
	private Component getContent() {
		HorizontalLayout content = new HorizontalLayout();
		// content.setFlexGrow(2, grid);
		content.setFlexGrow(4, form);
		content.addClassNames("content");
		content.setSizeFull();
		content.add(grid, form);
		return content;
	}
 
	private void configureGrid() {
		ValueProvider<UserDto, String> usernameProvider = reportModelDto -> String
				.valueOf(reportModelDto.getUserRoles());
 
		ComponentRenderer<Label, UserDto> userRolesRenderer = new ComponentRenderer<>(
				reportModelDto -> {
					String value = String.valueOf(reportModelDto.getUserRoles()).replace("[", "").replace("]", "")
							.replace("null,", "").replace("null", "");
					Label label = new Label(value);
					label.getStyle().set("color", "var(--lumo-body-text-color) !important");
					return label;
				});
 
		grid.setSelectionMode(SelectionMode.SINGLE);
		grid.setMultiSort(true, MultiSortPriority.APPEND);
		grid.setSizeFull();
		grid.setColumnReorderingAllowed(true);
 
		grid.addColumn(userRolesRenderer).setHeader("User Roles").setSortable(true).setResizable(true);
		grid.addColumn(UserDto::getUserName).setHeader("Username").setSortable(true).setResizable(true);
		grid.addColumn(UserDto::getName).setHeader("Name").setSortable(true).setResizable(true);
		grid.addColumn(UserDto::getUserEmail).setHeader("Email").setSortable(true).setResizable(true);
		grid.addColumn(UserDto::getUserPosition).setHeader("Organisation").setSortable(true).setResizable(true);
		grid.addColumn(UserDto::getUserOrganisation).setHeader("Position").setSortable(true).setResizable(true);
		grid.addColumn(UserDto::getRegion).setHeader("Region").setResizable(true).setSortable(true);
 
		grid.setVisible(true);
		grid.setWidthFull();
		grid.setHeightFull();
		grid.setAllRowsVisible(false);
 
		// Implement User level view after implementing User Roles/Security
//			if (UserProvider.getCurrent().hasUserRole(UserRole.ADMIN_SUPERVISOR)) {
//				dataProvider = DataProvider.fromStream(
//						FacadeProvider.getUserFacade().getIndexList(getCriteria(), null, null, null).stream().filter(e -> e.getDistrict().equals(UserProvider.getCurrent().getUser().getDistrict())));
//			} else if (UserProvider.getCurrent().hasUserRole(UserRole.ADMIN_SUPERVISOR)) {
//				dataProvider = DataProvider.fromStream(
//						FacadeProvider.getUserFacade().getIndexList(getCriteria(), null, null, null).stream().filter(null));
//			} else {
//				dataProvider = DataProvider.fromStream(
//						FacadeProvider.getUserFacade().getIndexList(getCriteria(), null, null, null).stream().filter(e -> e.getDistrict().equals(UserProvider.getCurrent().getUser().getDistrict())));
//			}
 
		List<UserDto> regions = FacadeProvider.getUserFacade().getIndexList(null, null, null, null).stream()
				.collect(Collectors.toList());
		dataView = grid.setItems(regions);
 
		grid.asSingleSelect().addValueChangeListener(event -> editContact(event.getValue()));
	}
 
	private void configureForm() {
		form = new UserForm();
		form.setSizeFull();
		form.getStyle().set("margin", "20px");
		form.addSaveListener(this::saveContact);
		form.addDeleteListener(this::deleteContact);
		form.addCloseListener(e -> closeEditor());
	}
 
	// TODO: Hide the filter bar on smaller screens
	public void addFilters() {
		HorizontalLayout layout = new HorizontalLayout();
		layout.setMargin(false);
		//layout.getStyle().set("margin", "15px");
		layout.setPadding(false);
		layout.setWidthFull();
 
		createUserButton.addClassName("resetButton");
//		createUserButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		layout.add(createUserButton);
		Icon createIcon = new Icon(VaadinIcon.PLUS_CIRCLE_O);
		createUserButton.setIcon(createIcon);
 
		exportUsersButton.addClassName("resetButton");
//		exportUsersButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		layout.add(exportUsersButton);
//		exportUsersButton.addClickListener(e -> {
// 
//		});
//		
		exportUsersButton.addClickListener(e -> {
	            // Retrieve the data from the Vaadin Grid
	            String data = getDataAsString(grid);

	            // Format the data in the desired format (e.g., CSV)
	            String formattedData = formatDataAsCsv(data);

	            // Create a temporary stream to write the formatted data
	            InputStream stream = new ByteArrayInputStream(formattedData.getBytes(StandardCharsets.UTF_8));

	            // Create a StreamResource to handle the file download
	            StreamResource resource = new StreamResource("data.csv", () -> stream);

	            // Trigger the file download in the user's browser
	            resource.setContentType("text/csv");
	            resource.setCacheTime(0);
//	            resource.setBufferSize(1024);

//	            FileDownloader downloader = new FileDownloader(resource);
//	            downloader.download();
	            
	            Anchor downloadLink = new Anchor(resource, "");
	            downloadLink.getElement().setAttribute("download", true);
	            downloadLink.getElement().getStyle().set("display", "none");

	            
	            Notification.show("File download initiated",
	                    3000,
	                    Notification.Position.BOTTOM_CENTER)
	                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
	        });

	  
		Icon exportUsersButtonIcon = new Icon(VaadinIcon.UPLOAD_ALT);
		exportUsersButton.setIcon(exportUsersButtonIcon);
 
		exportRolesButton.addClassName("resetButton");
//		exportRolesButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		Icon exportRolesButtonIcon = new Icon(VaadinIcon.USER_CHECK);
		exportRolesButton.setIcon(exportRolesButtonIcon);
		layout.add(exportRolesButton);
 
		bulkModeButton.addClassName("resetButton");
//		bulkModeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		Icon bulkModeButtonnIcon = new Icon(VaadinIcon.CLIPBOARD_CHECK);
		bulkModeButton.setIcon(bulkModeButtonnIcon);
		layout.add(bulkModeButton);
		bulkModeButton.addClickListener(e -> grid.setSelectionMode(Grid.SelectionMode.MULTI));
 
		searchField.setWidth("20%");
		searchField.addClassName("filterBar");
		searchField.setPlaceholder("Search");
		searchField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
		searchField.setValueChangeMode(ValueChangeMode.EAGER);
 
		searchField.addValueChangeListener(e -> {
 
		});
 
		layout.add(searchField);
		
		ComboBox activeBox = new ComboBox<>("Active?");
		activeBox.setPlaceholder("Active?");
		activeBox.setItems(ACTIVE_FILTER, INACTIVE_FILTER);
		layout.add(activeBox);
		
		add(layout);
 
		createUserButton.addClickListener(click -> 
//		addContact()
		{
			Dialog dialog = new Dialog();
		dialog.setCloseOnEsc(false);
		dialog.setCloseOnOutsideClick(false);
		Paragraph tect = new  Paragraph("dialog");
		dialog.add(form);
		dialog.open();
		add(dialog);
		}
		);
 
	}
 
	private String formatDataAsCsv(String data) {
		// TODO Auto-generated method stub
		return null;
	}

	private String getDataAsString(Grid<UserDto> grid2) {
		// TODO Auto-generated method stub
		return null;
	}

	public void editContact(UserDto contact) {
		if (contact == null) {
			closeEditor();
		} else {
			
			form.setUser(contact);
			form.setVisible(true);
			form.setSizeFull();
			grid.setVisible(false);
			setFiltersVisible(false);
			addClassName("editing");
		}
	}
	
	private void closeEditor() {
		form.setUser(null);
		form.setVisible(false);
		setFiltersVisible(true);
		grid.setVisible(true);
		removeClassName("editing");
	}
 
	private void setFiltersVisible(boolean state) {
		createUserButton.setVisible(state);
		exportUsersButton.setVisible(state);
		exportRolesButton.setVisible(state);
		bulkModeButton.setVisible(state);
		searchField.setVisible(state);
	}
 
	private void addContact() {
		grid.asSingleSelect().clear();
		editContact(new UserDto());
	}
 
	private void saveContact(UserForm.SaveEvent event) {
		FacadeProvider.getUserFacade().saveUser(event.getContact());
		// updateList();
		closeEditor();
	}
 
	private void deleteContact(UserForm.DeleteEvent event) {
		// FacadeProvider.getUserFacade(). .getContact());
		// updateList();
		closeEditor();
	}
 
}