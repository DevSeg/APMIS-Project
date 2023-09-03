package com.cinoteck.application.views.reports;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.cinoteck.application.UserProvider;
import com.cinoteck.application.views.campaigndata.CampaignFormElementImportance;
import com.flowingcode.vaadin.addons.gridexporter.GridExporter;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.MultiSortPriority;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;

import com.vaadin.flow.component.grid.Grid.Column;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.Language;
import de.symeda.sormas.api.campaign.CampaignJurisdictionLevel;
import de.symeda.sormas.api.campaign.CampaignReferenceDto;
import de.symeda.sormas.api.campaign.data.CampaignFormDataIndexDto;
import de.symeda.sormas.api.campaign.data.translation.TranslationElement;
import de.symeda.sormas.api.campaign.form.CampaignFormElement;
import de.symeda.sormas.api.campaign.form.CampaignFormElementType;
import de.symeda.sormas.api.campaign.form.CampaignFormMetaDto;
import de.symeda.sormas.api.campaign.form.CampaignFormMetaReferenceDto;
import de.symeda.sormas.api.campaign.form.CampaignFormTranslations;
import de.symeda.sormas.api.campaign.statistics.CampaignStatisticsCriteria;
import de.symeda.sormas.api.campaign.statistics.CampaignStatisticsDto;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.infrastructure.area.AreaReferenceDto;
import de.symeda.sormas.api.infrastructure.community.CommunityReferenceDto;
import de.symeda.sormas.api.infrastructure.district.DistrictReferenceDto;
import de.symeda.sormas.api.infrastructure.region.RegionReferenceDto;
import de.symeda.sormas.api.user.UserType;

@SuppressWarnings("serial")
@Route(layout = ReportView.class)
public class AggregateReportView extends VerticalLayout implements RouterLayout {
	ComboBox<String> campaignYear = new ComboBox<>();
	ComboBox<CampaignReferenceDto> campaignz = new ComboBox<>();
	CampaignStatisticsCriteria criteria;
	ComboBox<String> groupBy = new ComboBox<>();
	ComboBox<CampaignFormMetaReferenceDto> campaignFormCombo = new ComboBox<>();
	ComboBox<AreaReferenceDto> regionCombo = new ComboBox<>();
	ComboBox<RegionReferenceDto> provinceCombo = new ComboBox<>();
	ComboBox<DistrictReferenceDto> districtCombo = new ComboBox<>();
	ComboBox<CommunityReferenceDto> clusterCombo = new ComboBox<>();
	ComboBox<CampaignFormElementImportance> importanceSwitcher = new ComboBox<>();

	Button resetHandler = new Button();
	
	Button exportReport = new Button();

	private Grid<CampaignStatisticsDto> grid = new Grid<>(CampaignStatisticsDto.class, false);
	ListDataProvider<CampaignStatisticsDto> dataProvider;
	List<AreaReferenceDto> regions;
	List<RegionReferenceDto> provinces;
	List<DistrictReferenceDto> districts;
	List<CommunityReferenceDto> communities;
	List<CampaignFormMetaReferenceDto> campaignForms;
	private CampaignFormMetaDto formMetaReference;

	private Consumer<CampaignFormMetaReferenceDto> formMetaChangedCallback;

	
	Anchor anchor = new Anchor("", I18nProperties.getCaption(Captions.export));
	Icon icon = VaadinIcon.UPLOAD_ALT.create();
	
	
	public AggregateReportView() {
		criteria = new CampaignStatisticsCriteria();
		criteria.setGroupingLevel(CampaignJurisdictionLevel.AREA);
		addFilter();
		configureGrid(criteria);
	}

	public void configureGrid(CampaignStatisticsCriteria criteria) {
		this.criteria = criteria;
		grid.setSelectionMode(SelectionMode.NONE);
		grid.setMultiSort(true, MultiSortPriority.APPEND);
		setSizeFull();
		grid.setColumnReorderingAllowed(true);
		grid.addColumn(CampaignStatisticsDto.CAMPAIGN).setHeader(I18nProperties.getCaption(Captions.Campaign))
				.setSortable(true).setResizable(true).setAutoWidth(true)
				.setTooltipGenerator(e -> I18nProperties.getCaption(Captions.Campaign));
		grid.addColumn(CampaignStatisticsDto.FORM).setHeader(I18nProperties.getCaption(Captions.formname)).setSortable(true).setResizable(true)
				.setAutoWidth(true).setTooltipGenerator(e -> I18nProperties.getCaption(Captions.formname));
		grid.addColumn(CampaignStatisticsDto.AREA).setHeader(I18nProperties.getCaption(Captions.area)).setSortable(true).setResizable(true)
				.setAutoWidth(true).setTooltipGenerator(e -> I18nProperties.getCaption(Captions.area));
		grid.addColumn(CampaignStatisticsDto.REGION).setHeader(I18nProperties.getCaption(Captions.region)).setSortable(true).setResizable(true)
				.setAutoWidth(true).setTooltipGenerator(e -> I18nProperties.getCaption(Captions.region)).setVisible(false);
		grid.addColumn(CampaignStatisticsDto.DISTRICT).setHeader(I18nProperties.getCaption(Captions.district)).setSortable(true).setResizable(true)
				.setAutoWidth(true).setTooltipGenerator(e -> I18nProperties.getCaption(Captions.district)).setVisible(false);
		grid.addColumn(CampaignStatisticsDto.COMMUNITY).setHeader(I18nProperties.getCaption(Captions.community)).setSortable(true).setResizable(true)
				.setAutoWidth(true).setTooltipGenerator(e -> I18nProperties.getCaption(Captions.community)).setVisible(false);
		grid.addColumn(CampaignStatisticsDto.FORM_COUNT).setHeader(I18nProperties.getCaption(Captions.formCount)).setSortable(true).setResizable(true)
				.setTooltipGenerator(e -> I18nProperties.getCaption(Captions.formCount));

		grid.setVisible(true);

		dataProvider = DataProvider.fromStream(getGridData().stream());

		grid.setDataProvider(dataProvider);
		
		GridExporter<CampaignStatisticsDto> exporter = GridExporter.createFor(grid);
		exporter.setAutoAttachExportButtons(false);

		exporter.setTitle(I18nProperties.getCaption(Captions.campaignDataInformation));
		exporter.setFileName(
				"Aggregate Report" + new SimpleDateFormat("yyyyddMM").format(Calendar.getInstance().getTime()));

		anchor.setHref(exporter.getCsvStreamResource());
		anchor.getElement().setAttribute("download", true);
		anchor.setClassName("exportJsonGLoss");
		anchor.setId("campDatAnchor");

		anchor.getStyle().set("width", "100px");

		icon.getStyle().set("margin-right", "8px");
		icon.getStyle().set("font-size", "10px");

		anchor.getElement().insertChild(0, icon.getElement());
		add(grid);

	}

	public void addFilter() {
		UserProvider user = new UserProvider();
//		UserDto user = UserProvider.getCurrent().getUser();
		AreaReferenceDto userArea = user.getUser().getArea();
		RegionReferenceDto userRegion = user.getUser().getRegion();
		DistrictReferenceDto userDistrict = user.getUser().getDistrict();
		CommunityReferenceDto userCommunity = null;
		if (userArea != null) {
			regionCombo.setEnabled(false);
			provinceCombo.setItems(FacadeProvider.getRegionFacade().getAllActiveByArea(userArea.getUuid()));
			if (userRegion != null) {
				provinceCombo.setEnabled(false);
				districtCombo.setItems(FacadeProvider.getDistrictFacade().getAllActiveByRegion(userRegion.getUuid()));
				if (userDistrict != null) {
					districtCombo.setEnabled(false);
					// communityFilter.addItems(FacadeProvider.getCommunityFacade().getAllActiveByDistrict(userDistrict.getUuid()));
					// if (userCommunity != null) {
					// communityFilter.setEnabled(false);
					// }
				}
			}
		}

		criteria.setRegion(user.getUser().getRegion());// .setArea(user.getArea());
		criteria.setDistrict(user.getUser().getDistrict());
//		criteria.region(user.getRegion());// .setRegion(user.getRegion());
//		criteria.district(user.getDistrict()); // .setDistrict(user.getDistrict());
////		

		Button displayFilters = new Button(I18nProperties.getCaption(Captions.showFilters), new Icon(VaadinIcon.SLIDERS));

		HorizontalLayout actionButtonlayout = new HorizontalLayout();
		actionButtonlayout.setVisible(true);
		actionButtonlayout.setAlignItems(Alignment.END);

		actionButtonlayout.setClassName("row pl-3");
		actionButtonlayout.add(campaignYear, campaignz, groupBy, campaignFormCombo, regionCombo, provinceCombo,
				districtCombo, importanceSwitcher, resetHandler,exportReport, anchor);

		displayFilters.addClickListener(e -> {
			if (!actionButtonlayout.isVisible()) {
				actionButtonlayout.setVisible(true);
				displayFilters.setText(I18nProperties.getCaption(Captions.hideFilters));
			} else {
				actionButtonlayout.setVisible(false);
				displayFilters.setText(I18nProperties.getCaption(Captions.showFilters));
			}
		});

		campaignYear.setLabel(I18nProperties.getCaption(Captions.campaignYear));
		campaignYear.getStyle().set("padding-top", "0px !important");
		campaignYear.getStyle().set("width", "145px !important");

		campaignz.setLabel(I18nProperties.getCaption(Captions.Campaign));
		campaignz.getStyle().set("padding-top", "0px !important");
		campaignz.getStyle().set("width", "145px !important");

		
		groupBy.setLabel(I18nProperties.getCaption(Captions.campaignDiagramGroupBy));
		groupBy.getStyle().set("padding-top", "0px !important");
		groupBy.getStyle().set("width", "145px !important");


		// Initialize Item lists
		List<CampaignReferenceDto> campaigns = FacadeProvider.getCampaignFacade().getAllActiveCampaignsAsReference();
		CampaignReferenceDto lastStarted = FacadeProvider.getCampaignFacade().getLastStartedCampaign();
		List<String> camYearList = campaigns.stream().map(CampaignReferenceDto::getCampaignYear).distinct()
				.collect(Collectors.toList());

		campaignYear.setItems(camYearList);
		campaignYear.setValue(lastStarted.getCampaignYear());

		List<CampaignReferenceDto> allCampaigns = campaigns.stream()
				.filter(c -> c.getCampaignYear().equals(campaignYear.getValue())).collect(Collectors.toList());

		campaignz.setItems(allCampaigns);
		campaignz.setValue(lastStarted);
		criteria.setCampaign(lastStarted);

//		if(userProvider.getUser().getUsertype() == UserType.EOC_USER) {
//			campaignPhase.setItems(CampaignPhase.INTRA, CampaignPhase.POST);
//			campaignPhase.setValue(CampaignPhase.INTRA);
//		}else {
//			campaignPhase.setItems(CampaignPhase.values());
//			campaignPhase.setValue(CampaignPhase.PRE);
//		}

//		criteria.campaign(lastStarted);

		campaignFormCombo.setLabel(I18nProperties.getCaption(Captions.campaignCampaignForm));
		campaignFormCombo.getStyle().set("padding-top", "0px !important");
		campaignFormCombo.getStyle().set("--vaadin-combo-box-overlay-width", "350px");
		campaignFormCombo.getStyle().set("width", "145px !important");



		List<CampaignFormMetaReferenceDto> campaignFormReferences_ = FacadeProvider.getCampaignFormMetaFacade()
				.getCampaignFormMetasAsReferencesByCampaign(campaignz.getValue().getUuid());
		
		
		campaignFormCombo.setItems(campaignFormReferences_);
		campaignFormCombo.addValueChangeListener(event -> {
			if (event.getValue() != null) {
				formMetaReference = FacadeProvider.getCampaignFormMetaFacade()
						.getCampaignFormMetaByUuid(event.getValue().getUuid());
				criteria.setCampaignFormMeta(event.getValue());

				reloadData();
				importanceSwitcher.setReadOnly(false);
				importanceSwitcher.clear();
			} else {

				importanceSwitcher.clear();
				importanceSwitcher.setReadOnly(true);

			}
		});

		regionCombo.setLabel(I18nProperties.getCaption(Captions.area));
		regionCombo.getStyle().set("padding-top", "0px !important");
		regionCombo.getStyle().set("width", "145px !important");

		regionCombo.setPlaceholder(I18nProperties.getCaption(Captions.area));
		regions = FacadeProvider.getAreaFacade().getAllActiveAsReference();
		regionCombo.setItems(regions);
		regionCombo.setEnabled(true);
		regionCombo.addValueChangeListener(e -> {
			criteria.setArea(e.getValue());
			reloadData();
			provinces = FacadeProvider.getRegionFacade().getAllActiveByArea(e.getValue().getUuid());
			provinceCombo.setItems(provinces);
			provinceCombo.setEnabled(true);

		});

		provinceCombo.setLabel(I18nProperties.getCaption(Captions.region));
		provinceCombo.getStyle().set("padding-top", "0px !important");
		provinceCombo.getStyle().set("width", "145px !important");

		provinceCombo.setPlaceholder(I18nProperties.getCaption(Captions.region));
		provinceCombo.setEnabled(false);
		provinceCombo.getStyle().set("padding-top", "0px");
		provinceCombo.setClassName("col-sm-6, col-xs-6");

		provinceCombo.addValueChangeListener(e -> {
			criteria.setRegion(e.getValue());
			reloadData();
			districts = FacadeProvider.getDistrictFacade().getAllActiveByRegion(e.getValue().getUuid());
			districtCombo.setItems(districts);
			districtCombo.setEnabled(true);

		});

		districtCombo.setLabel(I18nProperties.getCaption(Captions.district));
		districtCombo.getStyle().set("padding-top", "0px !important");
		districtCombo.getStyle().set("width", "145px !important");

		districtCombo.setPlaceholder(I18nProperties.getCaption(Captions.district));
		districtCombo.setEnabled(false);
		districtCombo.getStyle().set("padding-top", "0px");
		districtCombo.setClassName("col-sm-6, col-xs-6");
		districtCombo.addValueChangeListener(e -> {
			criteria.setDistrict(e.getValue());

			reloadData();
		});

		groupBy.setItems(

				I18nProperties.getCaption(Captions.Campaign_area), I18nProperties.getCaption(Captions.Campaign_region),
				I18nProperties.getCaption(Captions.Campaign_district)

		);

		importanceSwitcher.setLabel(I18nProperties.getCaption(Captions.importance));
		importanceSwitcher.getStyle().set("padding-top", "0px !important");
		importanceSwitcher.setClassName("col-sm-6, col-xs-6");
		importanceSwitcher.getStyle().set("width", "145px !important");

		importanceSwitcher.setPlaceholder(I18nProperties.getCaption(Captions.importance));
		importanceSwitcher.setItems(CampaignFormElementImportance.values());
//		importanceSwitcher.setClearButtonVisible(true);
		importanceSwitcher.setReadOnly(true);
		importanceSwitcher.setTooltipText(I18nProperties.getCaption(Captions.selectFormFirst));

		importanceSwitcher.addValueChangeListener(e -> {

			if (formMetaReference != null) {
				grid.removeAllColumns();
				configureGrid(criteria);

				final boolean allAndImportantFormElements = e.getValue() == CampaignFormElementImportance.ALL;
				final boolean onlyImportantFormElements = e.getValue() == CampaignFormElementImportance.IMPORTANT;

				final List<CampaignFormElement> campaignFormElements = formMetaReference.getCampaignFormElements();

				for (CampaignFormElement element : campaignFormElements) {

					if (element.isImportant() && onlyImportantFormElements) {
						String caption = null;
						if (caption == null) {
							caption = element.getCaption();
						}

						if (caption != null) {
							addCustomColumn(element.getId(), caption);
						}
					} else if (allAndImportantFormElements) {
						String caption = null;
						if (caption == null) {
							caption = element.getCaption();
						}
						if (caption != null) {
							addCustomColumn(element.getId(), caption);
						}
					} else if (element.isImportant()) {
						String caption = null;
						if (caption == null) {
							caption = element.getCaption();
						}
						if (caption != null) {
							addCustomColumn(element.getId(), caption);
						}
					}
				}
			}

		});

		campaignYear.addValueChangeListener(e -> {
			campaignz.clear();
			List<CampaignReferenceDto> allCampaigns_ = campaigns.stream()
					.filter(c -> c.getCampaignYear().equals(campaignYear.getValue())).collect(Collectors.toList());
			campaignz.setItems(allCampaigns_);
			campaignz.setValue(allCampaigns_.get(0));
		});

		campaignz.addValueChangeListener(e -> {
			if (e.getValue() != null) {
				criteria.setCampaign(e.getValue());
				reloadData();
				List<CampaignFormMetaReferenceDto> campaignFormReferences_byCampUUIDx = 
						FacadeProvider.getCampaignFormMetaFacade().getCampaignFormMetasAsReferencesByCampaign(e.getValue().getUuid());
				
				campaignFormCombo.clear();
				campaignFormCombo.setItems(campaignFormReferences_byCampUUIDx);
				
			}
		});

		groupBy.addValueChangeListener(e -> {

			// TODO: Improve the initialization of this code
			CampaignJurisdictionLevel groupingValue = CampaignJurisdictionLevel.AREA;
			if (e.getValue() != null) {
				String selectorValue = e.getValue().toString();
				if (selectorValue.equals(I18nProperties.getCaption(Captions.area))) {
					groupingValue = CampaignJurisdictionLevel.AREA;
				} else if (selectorValue.equals(I18nProperties.getCaption(Captions.region))) {
					groupingValue = CampaignJurisdictionLevel.REGION;
				} else if (selectorValue.equals(I18nProperties.getCaption(Captions.district))) {
					groupingValue = CampaignJurisdictionLevel.DISTRICT;
				} else {
					// TODO add throwable here to make sure user does not inject insto the system
				}
			}

			criteria.setGroupingLevel(groupingValue);
			setColumnsVisibility(groupingValue);
			reloadData();
		});

		resetHandler.setText(I18nProperties.getCaption(Captions.resetFilters));

		resetHandler.addClickListener(e -> {

//		    // Refresh the grid and update criteria as needed

			UI.getCurrent().getPage().reload();
//		    criteria.campaign(lastStarted);
//		    criteria.setFormType(campaignPhase.getValue().toString());
		});

		exportReport.setText(I18nProperties.getCaption(Captions.export));
		exportReport.addClickListener(e -> {
			anchor.getElement().callJsFunction("click");
		});
		anchor.getStyle().set("display", "none");

		
		VerticalLayout filterBlock = new VerticalLayout();
		filterBlock.setSpacing(true);
		filterBlock.setMargin(true);
		filterBlock.setClassName("campaignDataFilterParent");

		HorizontalLayout layout = new HorizontalLayout();
		layout.setAlignItems(Alignment.END);

		layout.add(displayFilters , actionButtonlayout);

		filterBlock.add(layout);

		add(filterBlock);
	}

	private Consumer<CampaignFormMetaReferenceDto> createFormMetaChangedCallback() {
		return formMetaReference -> {
			grid.removeAllColumns();
			grid.addColumns();
			if (formMetaReference != null) {
				CampaignFormMetaDto formMeta = FacadeProvider.getCampaignFormMetaFacade()
						.getCampaignFormMetaByUuid(formMetaReference.getUuid());
				Language userLanguage = UserProvider.getCurrent().getUser().getLanguage();
				CampaignFormTranslations translations = null;
				if (userLanguage != null) {
					translations = formMeta.getCampaignFormTranslations().stream()
							.filter(t -> t.getLanguageCode().equals(userLanguage.getLocale().toString())).findFirst()
							.orElse(null);
				}
//				final boolean onlyImportantFormElements = importanceFilterSwitcher.isImportantSelected();
				final List<CampaignFormElement> campaignFormElements = formMeta.getCampaignFormElements();
				for (CampaignFormElement element : campaignFormElements) {
					if (element.isImportant()) {// || !onlyImportantFormElements) {
						String type = element.getType();
						if (type != null) {
							CampaignFormElementType campaignFormElementType = CampaignFormElementType.fromString(type);
							if (campaignFormElementType == CampaignFormElementType.NUMBER
									|| campaignFormElementType == CampaignFormElementType.DECIMAL
									|| campaignFormElementType == CampaignFormElementType.RANGE
									|| campaignFormElementType == CampaignFormElementType.RADIOBASIC
									|| campaignFormElementType == CampaignFormElementType.CHECKBOX
									|| campaignFormElementType == CampaignFormElementType.CHECKBOXBASIC
									|| campaignFormElementType == CampaignFormElementType.RADIO
									|| campaignFormElementType == CampaignFormElementType.YES_NO) {
								String caption = null;
								if (translations != null) {
									caption = translations.getTranslations().stream()
											.filter(t -> t.getElementId().equals(element.getId()))
											.map(TranslationElement::getCaption).findFirst().orElse(null);
								}
								if (caption == null) {
									caption = element.getCaption();
								}

								if (caption != null) {
									addCustomColumn(element.getId(), caption);
								}
							}
						}
					}
				}
			}
			setColumnsVisibility(criteria.getGroupingLevel());
		};
	}

	public void addCustomColumn(String property, String caption) {
		Column<CampaignStatisticsDto> newColumn = grid.addColumn(
				e -> e.getStatisticsData().stream().filter(v -> v.getId().equals(property)).findFirst().orElse(null));
		newColumn.setSortable(false);
		newColumn.setHeader(caption);// .setCaption(caption);
		newColumn.setId(property);
		newColumn.setResizable(true);
		newColumn.setTooltipGenerator(e -> newColumn.getHeaderText());

	}

//	public void addCustomColumnx(String property, String caption) {
//	    Column<CampaignStatisticsDto> newColumn = grid.addColumn(dto -> dto.getStatisticsData()
//	            .stream()
//	            .filter(v -> v.getId().equals(property))
//	            .findFirst()
//	            .map(StatisticsData::getValue)
//	            .orElse(null))
//	        .setSortable(false)
//	        .setHeader(caption)
//	        .setKey(property);
//	}

	public void setColumnsVisibility(CampaignJurisdictionLevel groupingLevel) {
		setAreaColumnVisible(CampaignJurisdictionLevel.AREA.equals(groupingLevel)
				|| CampaignJurisdictionLevel.REGION.equals(groupingLevel));
		setRegionColumnVisible(CampaignJurisdictionLevel.REGION.equals(groupingLevel)
				|| CampaignJurisdictionLevel.DISTRICT.equals(groupingLevel)
				|| CampaignJurisdictionLevel.COMMUNITY.equals(groupingLevel));
		setDistrictColumnVisible(CampaignJurisdictionLevel.DISTRICT.equals(groupingLevel)
				|| CampaignJurisdictionLevel.COMMUNITY.equals(groupingLevel));
		setCommunityColumnVisible(CampaignJurisdictionLevel.COMMUNITY.equals(groupingLevel));
	}

	private void setAreaColumnVisible(boolean visible) {
		// getColumn(CampaignStatisticsDto.AREA).setHidden(!visible);
	}

	private void setRegionColumnVisible(boolean visible) {
		grid.getColumnByKey(CampaignStatisticsDto.REGION).setVisible(visible);

	}

	private void setDistrictColumnVisible(boolean visible) {
		grid.getColumnByKey(CampaignStatisticsDto.DISTRICT).setVisible(visible);
	}

	private void setCommunityColumnVisible(boolean visible) {
		grid.getColumnByKey(CampaignStatisticsDto.COMMUNITY).setVisible(visible);
	}

	public void reloadData() {

		dataProvider = DataProvider.fromStream(getGridData().stream());

		grid.setDataProvider(dataProvider);
//	     
	}

	public void setCriteria(CampaignStatisticsCriteria criteria) {
		this.criteria = criteria;
		reloadData();
	}

	private CampaignStatisticsCriteria getCriteria() {
		return criteria;
	}

	private List<CampaignStatisticsDto> getGridData() {
		return FacadeProvider.getCampaignStatisticsFacade().getCampaignStatistics(getCriteria());
	}

	protected void applyDependenciesOnNewValue(CampaignStatisticsCriteria criteria) {
		campaignFormCombo.clear();
		if (criteria.getCampaign() != null) {
			if (UserProvider.getCurrent().hasUserType(UserType.EOC_USER)) {
				campaignFormCombo.setItems(FacadeProvider.getCampaignFormMetaFacade()
						.getCampaignFormMetaAsReferencesByCampaignPostCamapaign(criteria.getCampaign().getUuid()));
			} else {
				campaignFormCombo.setItems(FacadeProvider.getCampaignFormMetaFacade()
						.getCampaignFormMetasAsReferencesByCampaign(criteria.getCampaign().getUuid()));
			}
		} else {
			campaignFormCombo
					.setItems(FacadeProvider.getCampaignFormMetaFacade().getAllCampaignFormMetasAsReferences());
		}
	}

	public void setFormMetaChangedCallback(Consumer<CampaignFormMetaReferenceDto> formMetaChangedCallback) {
		this.formMetaChangedCallback = formMetaChangedCallback;
		campaignFormCombo.addValueChangeListener(e -> formMetaChangedCallback.accept(e.getValue()));
	}
}