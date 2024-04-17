//package de.symeda.sormas.backend.infrastructure;
//
//import java.util.Date;
//
//import javax.persistence.Entity;
//import javax.persistence.EnumType;
//import javax.persistence.Enumerated;
//import javax.persistence.ManyToOne;
//import javax.persistence.Temporal;
//import javax.persistence.TemporalType;
//
//import de.symeda.sormas.api.AgeGroup;
//import de.symeda.sormas.api.person.Sex;
//import de.symeda.sormas.backend.campaign.Campaign;
//import de.symeda.sormas.backend.common.AbstractDomainObject;
//import de.symeda.sormas.backend.infrastructure.community.Community;
//import de.symeda.sormas.backend.infrastructure.district.District;
//import de.symeda.sormas.backend.infrastructure.region.Region;
//
//@Entity
//public class PopulationDataFaux extends AbstractDomainObject {
//
//
//	/**
//	 * 
//	 */
//	private static final long serialVersionUID = -613489460394159544L;
//
//	public static final String TABLE_NAME = "populationdatafaux";
//
//	public static final String REGION = "region";
//	public static final String DISTRICT = "district";
//	public static final String COMMUNITY = "community";
//	public static final String CAMPAIGN = "campaign";
//	public static final String SEX = "sex";
//	public static final String AGE_GROUP = "ageGroup";
//	public static final String POPULATION = "population";
//	public static final String COLLECTION_DATE = "collectionDate";
//	public static final String SELECTED = "selected";
//	public static final String MODALITY = "modality";
//
//
//	private Region region;
//	private District district;
//	private Community community;
//	private Sex sex;
//	private AgeGroup ageGroup;
//	private Integer population;
//	private Date collectionDate;
//	private Campaign campaign;
//	private boolean selected;
//	private String modality;
//
//	@ManyToOne(cascade = {})
//	public Region getRegion() {
//		return region;
//	}
//
//	public void setRegion(Region region) {
//		this.region = region;
//	}
//
//	@ManyToOne(cascade = {})
//	public District getDistrict() {
//		return district;
//	}
//
//	public void setDistrict(District district) {
//		this.district = district;
//	}
//
//	@ManyToOne(cascade = {})
//	public Community getCommunity() {
//		return community;
//	}
//
//	public void setCommunity(Community community) {
//		this.community = community;
//	}
//
//	@Enumerated(EnumType.STRING)
//	public Sex getSex() {
//		return sex;
//	}
//
//	public void setSex(Sex sex) {
//		this.sex = sex;
//	}
//
//	@Enumerated(EnumType.STRING)
//	public AgeGroup getAgeGroup() {
//		return ageGroup;
//	}
//
//	public void setAgeGroup(AgeGroup ageGroup) {
//		this.ageGroup = ageGroup;
//	}
//
//	public Integer getPopulation() {
//		return population;
//	}
//
//	public void setPopulation(Integer population) {
//		this.population = population;
//	}
//
//	@Temporal(TemporalType.TIMESTAMP)
//	public Date getCollectionDate() {
//		return collectionDate;
//	}
//
//	public void setCollectionDate(Date collectionDate) {
//		this.collectionDate = collectionDate;
//	}
//
//	
//	@ManyToOne(cascade = {})
//	public Campaign getCampaign() {
//		return campaign;
//	}
//
//	public void setCampaign(Campaign campaign) {
//		this.campaign = campaign;
//	}
//
//	public boolean isSelected() {
//		return selected;
//	}
//
//	public void setSelected(boolean selected) {
//		this.selected = selected;
//	}
//
//	public String getModality() {
//		return modality;
//	}
//
//	public void setModality(String modality) {
//		this.modality = modality;
//	}
//
//}
