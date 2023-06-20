package com.cinoteck.application.views.campaign;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.cinoteck.application.views.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.MultiSortPriority;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;


import de.symeda.sormas.api.EntityRelevanceStatus;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.campaign.CampaignDto;
import de.symeda.sormas.api.campaign.CampaignIndexDto;
import de.symeda.sormas.api.campaign.CampaignReferenceDto;
import de.symeda.sormas.api.campaign.form.CampaignFormMetaFacade;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.user.UserDto;
import de.symeda.sormas.api.utils.ValidationRuntimeException;

@PageTitle("All Campaigns")
@Route(value = "campaign", layout = MainLayout.class)
public class CampaignsView extends VerticalLayout {

	private Button filterDisplayToggle;
	private Button validateFormsButton;
	private Button createButton;
	private TextField searchField;
	private ComboBox<EntityRelevanceStatus> relevanceStatusFilter;
	VerticalLayout campaignsFilterLayout = new VerticalLayout();
	private Grid<CampaignDto> grid = new Grid<>(CampaignDto.class, false);
	private GridListDataView<CampaignDto> dataView;
	
	
	private CampaignForm campaignForm;
	CampaignDto camp;
	private List<CampaignReferenceDto> campaignName, campaignRound, campaignStartDate, campaignEndDate,
	campaignDescription;
	
	


	public CampaignsView() {
		setHeightFull();
		createFilterBar();
		campaignsGrid();
		configureForm();
		add(getContent());
		closeEditor();
	}

	private Component getContent() {
		HorizontalLayout content = new HorizontalLayout();
		// content.setFlexGrow(2, grid);
		content.setFlexGrow(4, campaignForm);
		content.addClassNames("content");
		content.setSizeFull();
		content.add(grid, campaignForm);
		return content;
	}

	private boolean matchesTerm() {
		// TODO Auto-generated method stub
		return false;
	}

	private void campaignsGrid() {
		
		grid.setSelectionMode(SelectionMode.SINGLE);
		grid.setMultiSort(true, MultiSortPriority.APPEND);
		grid.setSizeFull();
		grid.setColumnReorderingAllowed(true);
		grid.addColumn(CampaignDto.NAME).setHeader("Name").setSortable(true).setResizable(true);
		grid.addColumn(CampaignIndexDto.CAMPAIGN_STATUS).setHeader("Status").setSortable(true).setResizable(true);
		grid.addColumn(CampaignDto.START_DATE).setHeader("Start Date").setSortable(true).setResizable(true);
		grid.addColumn(CampaignDto.END_DATE).setHeader("End Date").setSortable(true).setResizable(true);
		grid.addColumn(CampaignDto.CAMPAIGN_YEAR).setHeader("Campaign Year").setSortable(true).setResizable(true);

		grid.setVisible(true);
		grid.setWidthFull();
//		grid.setHeightFull();
		grid.setAllRowsVisible(true);

		
	
//		VerticalLayout detailsLayout = new VerticalLayout();
//		detailsLayout.add(new Label("Additional information"));
//		details.setContent(detailsLayout);
//		
//		grid.addSelectionListener(event -> {
//		    Set<CampaignDto> selectedItems = event.getAllSelectedItems();
//		    if (!selectedItems.isEmpty()) {
//		    	CampaignDto selectedItem = selectedItems.iterator().next();
//		        detailsLayout.removeAll();
//		        detailsLayout.add(new Label("Additional information: " + selectedItem.getName()));
//		        details.setOpened(true);
//		    } else {
//		        details.setOpened(false);
//		    }
//		});
//		
		List<CampaignDto> campaigns = FacadeProvider.getCampaignFacade().getAllActive().stream()
				.collect(Collectors.toList());
		
		dataView = grid.setItems(campaigns);
		
		grid.asSingleSelect().addValueChangeListener(event -> editCampaign(event.getValue()));
	}
	
	private void configureForm() {
		campaignForm = new CampaignForm(camp);
		campaignForm.setSizeFull();
//		campaignForm.setVisible(false);
		campaignForm.getStyle().set("margin", "20px");
		campaignForm.addSaveListener(this::saveCampaign);
		campaignForm.addDeleteListener(this::deleteCampaign);
		campaignForm.addCloseListener(e -> closeEditor());
	}

	private void createFilterBar() {
		HorizontalLayout filterToggleLayout = new HorizontalLayout();
		filterToggleLayout.setAlignItems(Alignment.END);

		filterDisplayToggle = new Button("Show Filters");
		filterDisplayToggle.getStyle().set("margin-left", "12px");
		filterDisplayToggle.getStyle().set("margin-top", "12px");
		filterDisplayToggle.setIcon(new Icon(VaadinIcon.SLIDERS));

		HorizontalLayout filterLayout = new HorizontalLayout();
		filterLayout.getStyle().set("margin-left", "12px");
		filterLayout.setVisible(false);

		filterDisplayToggle.addClickListener(e -> {
			if (!filterLayout.isVisible()) {
				filterLayout.setVisible(true);
				filterDisplayToggle.setText("Hide Filters");

			} else {
				filterLayout.setVisible(false);
				filterDisplayToggle.setText("Show Filters");
			}

		});

		searchField = new TextField();
		searchField.setLabel("Search Campaign");
		searchField.setPlaceholder("Search");
		searchField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
		
		searchField.setValueChangeMode(ValueChangeMode.EAGER);
		searchField.addValueChangeListener(e -> dataView.addFilter(campaignsz -> {
			String searchTerm = searchField.getValue().trim();

			if (searchTerm.isEmpty())
				return true;

			boolean matchesFullName = matchesTerm();

			return matchesFullName;
		}));

		relevanceStatusFilter = new ComboBox<EntityRelevanceStatus>();
		relevanceStatusFilter.setLabel("Campaign Status");
		relevanceStatusFilter.setItems((EntityRelevanceStatus[]) EntityRelevanceStatus.values());

		validateFormsButton = new Button("Validate Forms", new Icon(VaadinIcon.CHECK_CIRCLE));
		validateFormsButton.addClickListener(e -> {
			try {
			FacadeProvider.getCampaignFormMetaFacade().validateAllFormMetas();
			Notification.show(I18nProperties.getString(Strings.messageAllCampaignFormsValid), 3000, Position.TOP_CENTER);
			}catch(ValidationRuntimeException ee) {
				
				Notification.show("All Camapaign Forms are not Valid, Please look through the Log", 8000, Position.MIDDLE);
				
				
			}
	
		});

		createButton = new Button("Add New Forms", new Icon(VaadinIcon.PLUS_CIRCLE));
		createButton.addClickListener(e-> {
			CreateCampaignDialog dialog = new CreateCampaignDialog();
			dialog.open();
		});
		filterLayout.add(searchField, relevanceStatusFilter);
		filterToggleLayout.add(filterDisplayToggle,filterLayout, validateFormsButton, createButton);
		
		campaignsFilterLayout.add(filterToggleLayout);

		add(campaignsFilterLayout);
	}

	

	public void editCampaign(CampaignDto campaign) {
		if (campaign == null) {
			campaignForm.setVisible(true);
			campaignForm.setSizeFull();
//			campaignsFilterLayout.setVisible(false);
			grid.setVisible(false);
			setFiltersVisible(false);
			addClassName("editing");
		} else {

		campaignForm.setCampaign(campaign);
		campaignForm.setVisible(true);
		campaignForm.setSizeFull();
//		campaignsFilterLayout.setVisible(false);
		grid.setVisible(false);
		setFiltersVisible(false);
		addClassName("editing");
		}

	}

	private void closeEditor() {
		campaignForm.setCampaign(null);
		campaignForm.setVisible(false);
		setFiltersVisible(true);
		grid.setVisible(true);
		removeClassName("editing");
	}
	
	private void setFiltersVisible(boolean state) {
		filterDisplayToggle.setVisible(state);
		createButton.setVisible(state);
		validateFormsButton.setVisible(state);
		relevanceStatusFilter.setVisible(state);
		searchField.setVisible(state);
	}

	private void addCampaign() {
		grid.asSingleSelect().clear();
		editCampaign(new CampaignDto());
	}

	private void saveCampaign(CampaignForm.SaveEvent event) {
		FacadeProvider.getCampaignFacade().saveCampaign(event.getCampaign()); 
		closeEditor();
	}

	private void deleteCampaign(CampaignForm.DeleteEvent event) {
		closeEditor();
	}

}
