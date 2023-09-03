package com.cinoteck.application.views.campaign;

import java.util.Date;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;

import com.cinoteck.application.LanguageSwitcher;
import com.cinoteck.application.UserProvider;
import com.cinoteck.application.views.MainLayout;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.dialog.DialogVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.MultiSortPriority;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import de.symeda.sormas.api.EntityRelevanceStatus;
import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.Language;
import de.symeda.sormas.api.campaign.CampaignCriteria;
import de.symeda.sormas.api.campaign.CampaignDto;
import de.symeda.sormas.api.campaign.CampaignIndexDto;
import de.symeda.sormas.api.campaign.CampaignLogDto;
import de.symeda.sormas.api.campaign.CampaignReferenceDto;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.infrastructure.area.AreaReferenceDto;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.api.utils.ValidationRuntimeException;

@PageTitle("APMIS-All Campaigns")
@Route(value = "campaign", layout = MainLayout.class)
public class CampaignsView extends VerticalLayout {

	private Button filterDisplayToggle;
	private Button validateFormsButton;
	private Button createButton;
	private TextField searchField;
	private ComboBox<EntityRelevanceStatus> relevanceStatusFilter;
	VerticalLayout campaignsFilterLayout = new VerticalLayout();

//	private Grid<CampaignDto> grid = new Grid<>(CampaignDto.class, false);
//	private GridListDataView<CampaignDto> dataView;

	private Grid<CampaignIndexDto> grid = new Grid<>(CampaignIndexDto.class, false);
	private GridListDataView<CampaignIndexDto> dataView;
	List<CampaignIndexDto> campaigns;

	CampaignCriteria criteria = new CampaignCriteria();
	List<CampaignIndexDto> indexList = FacadeProvider.getCampaignFacade().getIndexList(criteria, null, null, null);
	ListDataProvider<CampaignIndexDto> indexDataProvider = new ListDataProvider<>(indexList);

	private CampaignForm campaignForm;
	CampaignDto dto;
	private List<CampaignReferenceDto> campaignName, campaignRound, campaignStartDate, campaignEndDate,
			campaignDescription;

	private final UserProvider userProvider = new UserProvider();

	boolean isEditingModeActive;

	public CampaignsView() {
		if (I18nProperties.getUserLanguage() == null) {

			I18nProperties.setUserLanguage(Language.EN);
		} else {

			I18nProperties.setUserLanguage(userProvider.getUser().getLanguage());
			I18nProperties.getUserLanguage();
		}
		FacadeProvider.getI18nFacade().setUserLanguage(userProvider.getUser().getLanguage());
		setSpacing(false);
		setHeightFull();
		createFilterBar();
		campaignsGrid();

	}

	private boolean matchesTerm() {
		return false;
	}

	private void campaignsGrid() {
		criteria.relevanceStatus(EntityRelevanceStatus.ACTIVE);

		this.criteria = new CampaignCriteria();
		grid.setSelectionMode(SelectionMode.SINGLE);
		grid.setMultiSort(true, MultiSortPriority.APPEND);
		grid.setSizeFull();
		grid.setColumnReorderingAllowed(true);

		TextRenderer<CampaignIndexDto> startDateRenderer = new TextRenderer<>(dto -> {
			Date timestamp = dto.getStartDate();
			SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
			return dateFormat.format(timestamp);
		});

		TextRenderer<CampaignIndexDto> endDateRenderer = new TextRenderer<>(dto -> {
			Date timestamp = dto.getEndDate();
			SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
			return dateFormat.format(timestamp);
		});
		grid.addColumn(CampaignIndexDto.NAME).setHeader(I18nProperties.getCaption(Captions.name)).setSortable(true)
				.setResizable(true);
		grid.addColumn(CampaignIndexDto.CAMPAIGN_STATUS).setHeader(I18nProperties.getCaption(Captions.campaignStatus))
				.setSortable(true).setResizable(true);
		grid.addColumn(startDateRenderer).setHeader(I18nProperties.getCaption(Captions.Campaign_startDate))
				.setSortable(true).setResizable(true);
		grid.addColumn(endDateRenderer).setHeader(I18nProperties.getCaption(Captions.Campaign_endDate))
				.setSortable(true).setResizable(true);
		grid.addColumn(CampaignIndexDto.CAMPAIGN_YEAR).setHeader(I18nProperties.getCaption(Captions.campaignYear))
				.setSortable(true).setResizable(true);
		grid.addColumn(CampaignIndexDto.ARCHIVE).setHeader(I18nProperties.getCaption(Captions.relevanceStatus))
				.setSortable(true).setResizable(true);

		grid.setVisible(true);
		grid.setWidthFull();
		grid.setAllRowsVisible(true);
		criteria.relevanceStatus(EntityRelevanceStatus.ACTIVE);
		ListDataProvider<CampaignIndexDto> dataProvider = DataProvider
				.fromStream(FacadeProvider.getCampaignFacade().getIndexList(criteria, null, null, null).stream());

		dataView = grid.setItems(dataProvider);

		if (userProvider.hasUserRight(UserRight.CAMPAIGN_EDIT)) {

			grid.asSingleSelect().addValueChangeListener(event -> editCampaign(event.getValue()));

		}
		add(grid);
	}

	private void refreshGridData() {
		ListDataProvider<CampaignIndexDto> dataProvider = DataProvider
				.fromStream(FacadeProvider.getCampaignFacade().getIndexList(criteria, null, null, null).stream());
		dataView = grid.setItems(dataProvider);
	}

	private Component createStatusComponent(CampaignDto item) {

		CampaignIndexDto indexDto = indexDataProvider.getItems().stream()
				.filter(index -> index.getCampaignStatus().equals(item.getCampaignStatus())).findFirst().orElse(null);

		String statusText = indexDto != null ? indexDto.getCampaignStatus() : "";
		Label statusLabel = new Label(statusText);

		return statusLabel;
	}

	private void createFilterBar() {
		HorizontalLayout filterToggleLayout = new HorizontalLayout();
		filterToggleLayout.setAlignItems(Alignment.END);

		filterDisplayToggle = new Button(I18nProperties.getCaption(Captions.hideFilters));
		filterDisplayToggle.getStyle().set("margin-left", "12px");
		filterDisplayToggle.getStyle().set("margin-top", "12px");
		filterDisplayToggle.setIcon(new Icon(VaadinIcon.SLIDERS));

		HorizontalLayout filterLayout = new HorizontalLayout();
		filterLayout.getStyle().set("margin-left", "12px");
		filterLayout.setVisible(true);

		filterDisplayToggle.addClickListener(e -> {
			if (!filterLayout.isVisible()) {
				filterLayout.setVisible(true);
				filterDisplayToggle.setText(I18nProperties.getCaption(Captions.hideFilters));

			} else {
				filterLayout.setVisible(false);
				filterDisplayToggle.setText(I18nProperties.getCaption(Captions.showFilters));
			}

		});

		searchField = new TextField();
		searchField.setLabel(I18nProperties.getCaption(Captions.campaignSearch));
		searchField.setPlaceholder(I18nProperties.getCaption(Captions.actionSearch));
		searchField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
		searchField.setClassName("col-sm-6, col-xs-6");

		searchField.setValueChangeMode(ValueChangeMode.EAGER);
		searchField.addValueChangeListener(e -> dataView.addFilter(search -> {
			String searchTerm = searchField.getValue().trim();
			if (searchTerm.isEmpty())
				return true;
			boolean matchesDistrictName = String.valueOf(search.getName()).toLowerCase()
					.contains(searchTerm.toLowerCase());
			return matchesDistrictName;
		}));

		relevanceStatusFilter = new ComboBox<EntityRelevanceStatus>();
		relevanceStatusFilter.setLabel(I18nProperties.getCaption(Captions.campaignStatus));
		relevanceStatusFilter.setItems((EntityRelevanceStatus[]) EntityRelevanceStatus.values());
		relevanceStatusFilter.setClearButtonVisible(true);
		relevanceStatusFilter.setClassName("col-sm-6, col-xs-6");

		relevanceStatusFilter.addValueChangeListener(e -> {

			criteria.relevanceStatus(e.getValue()); // Set the selected relevance status in the criteria object
			refreshGridData();

		});

		validateFormsButton = new Button(I18nProperties.getCaption(Captions.campaignValidateForms),
				new Icon(VaadinIcon.CHECK_CIRCLE));
		validateFormsButton.setClassName("col-sm-6, col-xs-6");
		validateFormsButton.addClickListener(e -> {
			try {
				FacadeProvider.getCampaignFormMetaFacade().validateAllFormMetas();
				Notification.show(I18nProperties.getString(Strings.messageAllCampaignFormsValid), 3000,
						Position.TOP_CENTER);
			} catch (ValidationRuntimeException ee) {

				Notification.show(I18nProperties.getString(Strings.messageAllCampaignFormsNotValid), 8000,
						Position.MIDDLE);

			}

		});

		createButton = new Button(I18nProperties.getCaption(Captions.campaignNewCampaign),
				new Icon(VaadinIcon.PLUS_CIRCLE));
		createButton.setClassName("col-sm-6, col-xs-6");
		createButton.addClickListener(e -> {
			
			isEditingModeActive= true;
		
			
			CampaignForm formLayout = new CampaignForm(dto);
			formLayout.editMode = true;
			newCampaign(dto);
		});
		filterLayout.add(searchField, relevanceStatusFilter);

		if (userProvider.hasUserRight(UserRight.CAMPAIGN_EDIT)) {
			filterToggleLayout.add(filterDisplayToggle, filterLayout, validateFormsButton, createButton);
		} else {
			filterToggleLayout.add(filterDisplayToggle, filterLayout);
		}

		filterToggleLayout.setClassName("row pl-3");
		campaignsFilterLayout.add(filterToggleLayout);

		add(campaignsFilterLayout);
	}

	private void editCampaign(CampaignIndexDto selected) {
		selected = grid.asSingleSelect().getValue();
		if (selected != null) {
			CampaignDto formData = FacadeProvider.getCampaignFacade().getByUuid(selected.getUuid());
			openFormLayout(formData);
		}
	}

	private void newCampaign(CampaignDto formData) {
		
		System.out.println(formData + "formn data in configure ");
		CampaignForm formLayout = new CampaignForm(formData);
		formLayout.setCampaign(formData);

		formLayout.addSaveListener(this::saveCampaign);
		formLayout.addOpenCloseListener(this::openCloseCampaign);
		formLayout.addRoundChangeListener(this::roundChange);
		Dialog dialog = new Dialog();
		dialog.add(formLayout);
		dialog.setHeaderTitle(I18nProperties.getCaption(Captions.campaignNewCampaign));
		dialog.setSizeFull();
		dialog.open();
		dialog.setCloseOnEsc(false);
		dialog.setCloseOnOutsideClick(false);
		dialog.setModal(true);
		dialog.setClassName("formI");
	}

	private void openFormLayout(CampaignDto formData) {
		Dialog dialog = new Dialog();
		CampaignForm formLayout = new CampaignForm(formData);
		formLayout.setCampaign(formData);
		formLayout.addSaveListener(this::saveCampaign);
		formLayout.addArchiveListener(this::archiveDearchiveCampaign);
		formLayout.addPublishListener(this::publishUnpublishCampaign);
		formLayout.addLogListener(this::logButton);
		formLayout.addOpenCloseListener(this::openCloseCampaign);
		formLayout.addDeleteListener(this::deleteCampaign);
		formLayout.addDuplicateListener(this::duplicateCampaign);
		formLayout.addRoundChangeListener(this::roundChange);
		dialog.add(formLayout);
		dialog.setHeaderTitle(I18nProperties.getCaption(Captions.Campaign_edit) + " | " + formData.getName());
		dialog.setSizeFull();
		dialog.open();
		dialog.setCloseOnEsc(false);
		dialog.setCloseOnOutsideClick(false);
		dialog.setModal(true);
		dialog.setClassName("formI");
	}

	private void setFiltersVisible(boolean state) {
		filterDisplayToggle.setVisible(state);
		createButton.setVisible(state);
		validateFormsButton.setVisible(state);
		relevanceStatusFilter.setVisible(state);
		searchField.setVisible(state);
	}

	private void saveCampaign(CampaignForm.SaveEvent event) {
		CampaignForm forLayout =  event.getSource();
		if(dto == null) {
//			dto = new CampaignDto();
//			
//			// Assuming you have a LocalDate named localDate
//			LocalDate localDate = forLayout.startDate.getValue();
//			Date date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
//
//			dto.setUuid(forLayout.creatingUuid.getValue());
//			dto.setCampaignYear(forLayout.campaaignYear.getValue());
//			dto.setName(forLayout.campaignName.getValue());
//			dto.setStartDate(date);
			
			
			System.out.println(event.getCampaign()+ "save new     event campaign " + forLayout);

			FacadeProvider.getCampaignFacade().saveCampaign(event.getCampaign());


//			FacadeProvider.getCampaignFacade().saveCampaign(dto);
		
		}else {

			System.out.println(event.getCampaign()+ "save event campaign " + forLayout);
			FacadeProvider.getCampaignFacade().saveCampaign(event.getCampaign());

		}
		
	
	}

	

	private void roundChange(CampaignForm.RoundChangeEvent event) {

		if (event.getSource().round.getValue() == "Training") {
			System.out.println("yyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyy");
			event.getSource().campaignName.setValue(event.getSource().campaignName.getValue() + " {T}");
		}

	}

	private void archiveDearchiveCampaign(CampaignForm.ArchiveEvent event) {
		ConfirmDialog dialog = new ConfirmDialog();
		dialog.setCancelable(true);
		dialog.addCancelListener(e -> dialog.close());
		dialog.setRejectable(true);
		dialog.setRejectText("No");
		dialog.addRejectListener(e -> dialog.close());
		dialog.setConfirmText("Yes");

		CampaignForm formLayout = (CampaignForm) event.getSource();

		boolean isArchived = FacadeProvider.getCampaignFacade().isArchived(event.getCampaign().getUuid());

		if (isArchived) {
//			formLayout.archiveDearchive.setText("De-archive");
			dialog.setHeader("De-Archive Campaign");
			dialog.setText(
					"Are you sure you want to de-archive this campaign? This will make it appear in the active campaign directory again.");
			dialog.addConfirmListener(e -> {
				FacadeProvider.getCampaignFacade().archiveOrDearchiveCampaign(event.getCampaign().getUuid(), false);
				formLayout.updateArchiveButtonText(false);
			});

		} else {
//			formLayout.archiveDearchive.setText("Archive");

			dialog.setHeader("Archive Campaign");
			dialog.setText(
					"Are you sure you want to Archive this campaign? This will make it not appear in the normal campaign directory again.");
			dialog.addConfirmListener(e -> {
				FacadeProvider.getCampaignFacade().archiveOrDearchiveCampaign(event.getCampaign().getUuid(), true);
				formLayout.updateArchiveButtonText(true);
			});

		}
		dialog.open();
	}

	private void publishUnpublishCampaign(CampaignForm.PublishUnpublishEvent event) {
		ConfirmDialog dialog = new ConfirmDialog();
		dialog.setCancelable(true);
		dialog.addCancelListener(e -> dialog.close());
		dialog.setRejectable(true);
		dialog.setRejectText("No");
		dialog.addRejectListener(e -> dialog.close());
		dialog.setConfirmText("Yes");
		dialog.open();
		CampaignForm formLayout = (CampaignForm) event.getSource();
		boolean isPublished = FacadeProvider.getCampaignFacade().isPublished(event.getCampaign().getUuid());

		if (isPublished) {

			dialog.setHeader("Publish Campaign");
			dialog.setText(
					"Are you sure you want to Publish Report from this campaign? This will make the Report Available in Aggregate Reports.");
			dialog.addConfirmListener(e -> {
				FacadeProvider.getCampaignFacade().publishandUnPublishCampaign(event.getCampaign().getUuid(), false);
				formLayout.updatePublishButtonText(false);
			});

		} else {
			dialog.setHeader("Un-Publish Campaign");
			dialog.setText(
					"Are you sure you want to Un-Publish Report from this campaign? This will make the Report Un-Available in Aggregate Reports.");
			dialog.addConfirmListener(e -> {
				FacadeProvider.getCampaignFacade().publishandUnPublishCampaign(event.getCampaign().getUuid(), true);
				formLayout.updatePublishButtonText(true);
			});

		}
		formLayout.getChildren().forEach(child -> child.getElement().executeJs("this.requestLayout()"));

	}

	private void logButton(CampaignForm.LogCampaignEvent event) {

		Dialog dialog = new Dialog();
//		dialog.setCancelable(true);
//		dialog.setConfirmText("Close");
//		dialog.addCancelListener(e -> dialog.close());
		dialog.setWidthFull();
		dialog.open();

		CampaignForm formLayout = (CampaignForm) event.getSource();

		dialog.setHeaderTitle("Campaign Log");

		Grid<CampaignLogDto> grid = new Grid<>(CampaignLogDto.class, false);
		grid.setItems(FacadeProvider.getCampaignFacade().getAuditLog(FacadeProvider.getCampaignFacade().getReferenceByUuid(event.getCampaign().getUuid())));
//	        grid.setSelectionMode(Grid.SelectionMode.MULTI);
		grid.addColumn(CampaignLogDto::getCreatingUser_string).setHeader("User").setAutoWidth(true);
		grid.addColumn(CampaignLogDto::getAction).setHeader("Action").setAutoWidth(true);
		grid.addColumn(CampaignLogDto::getActionDate).setHeader("Timestamp").setAutoWidth(true);
		grid.setWidthFull();

//		grid.getStyle().set("width", "auto").set("max-width", "100%");

		dialog.add(grid);
//		dialog.addConfirmListener(e -> {
//			FacadeProvider.getCampaignFacade().publishandUnPublishCampaign(event.getCampaign().getUuid(), false);
//			formLayout.updatePublishButtonText(false);
//		});

		formLayout.getChildren().forEach(child -> child.getElement().executeJs("this.requestLayout()"));

//		Dialog dialogxd = new Dialog();
//
//		dialogxd.add(createDialogContent(dialogxd));
//
//		dialogxd.addThemeVariants(DialogVariant.LUMO_NO_PADDING);
//		CampaignForm formLayout = (CampaignForm) event.getSource();
//		formLayout.getChildren().forEach(child -> child.getElement().executeJs("this.requestLayout()"));

	}


	private void openCloseCampaign(CampaignForm.OpenCloseEvent event) {
		ConfirmDialog dialog = new ConfirmDialog();
		dialog.setCancelable(true);
		dialog.addCancelListener(e -> dialog.close());
		dialog.setRejectable(true);
		dialog.setRejectText("No");
		dialog.addRejectListener(e -> dialog.close());
		dialog.setConfirmText("Yes");
		dialog.open();
		CampaignForm formLayout = (CampaignForm) event.getSource();
		boolean isOpened = FacadeProvider.getCampaignFacade().isClosedd(event.getCampaign().getUuid());

		if (isOpened) {

			dialog.setHeader("Open Campaign");
			dialog.setText("Are you sure you want to Open this campaign? This will make this campaign status Open.");
			dialog.addConfirmListener(e -> {
				FacadeProvider.getCampaignFacade().closeandOpenCampaign(event.getCampaign().getUuid(), false);
				formLayout.updateOpenCloseButtonText(false);
			});

		} else {
			dialog.setHeader("Close Campaign");
			dialog.setText(
					"Are you sure you want to Close this campaign?  This will make this campaign status Closed.");
			dialog.addConfirmListener(e -> {
				FacadeProvider.getCampaignFacade().closeandOpenCampaign(event.getCampaign().getUuid(), true);
				formLayout.updateOpenCloseButtonText(true);
			});

		}
		formLayout.getChildren().forEach(child -> child.getElement().executeJs("this.requestLayout()"));
	}

	private void deleteCampaign(CampaignForm.DeleteEvent event) {
		ConfirmDialog dialog = new ConfirmDialog();
		dialog.setCancelable(true);
		dialog.addCancelListener(e -> dialog.close());
		dialog.setRejectable(true);
		dialog.setRejectText("No");
		dialog.addRejectListener(e -> dialog.close());
		dialog.setConfirmText("Yes");
		dialog.open();
//		CampaignForm formLayout = (CampaignForm) event.getSource();
//	    boolean isOpened = FacadeProvider.getCampaignFacade().isClosedd(event.getCampaign().getUuid());
		dialog.setHeader("Delete Campaign");
		dialog.setText(
				"Are you sure you want to Delete this campaign? This will make it Un-Available in the normal campaign directory again.");
		dialog.addConfirmListener(e -> {
			FacadeProvider.getCampaignFacade().deleteCampaign(event.getCampaign().getUuid());
			UI.getCurrent().getPage().reload();
		});

	}

	private void duplicateCampaign(CampaignForm.DuplicateEvent event) {
		UserProvider usr = new UserProvider();
		ConfirmDialog dialog = new ConfirmDialog();
		dialog.setCancelable(true);
		dialog.addCancelListener(e -> dialog.close());
		dialog.setRejectable(true);
		dialog.setRejectText("No");
		dialog.addRejectListener(e -> dialog.close());
		dialog.setConfirmText("Yes");
		dialog.open();
//		CampaignForm formLayout = (CampaignForm) event.getSource();
//	    boolean isOpened = FacadeProvider.getCampaignFacade().isClosedd(event.getCampaign().getUuid());
		dialog.setHeader("Duplicate Campaign");
		dialog.setText("Are you sure you want to Clone this campaign? .");
		dialog.addConfirmListener(e -> {
			FacadeProvider.getCampaignFacade().cloneCampaign(event.getCampaign().getUuid(),
					usr.getUser().getUserName());
			UI.getCurrent().getPage().reload();
		});

	}

}