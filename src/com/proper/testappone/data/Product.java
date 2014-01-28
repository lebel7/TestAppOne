package com.proper.testappone.data;

public class Product implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	private int ProductId;
	private String SuppCode;
	private String SupplierCat;
	private String Format;
	private String Artist;
	private String Title;
	private String ShortDescription;
	private String Barcode;
	private int OnHand;
	private float DealerPrice;
	private String BinNo;
	private float Price1;
	private int OutOfStock;
	
	public Product() {
	}

	public Product(int productId, String suppCode, String supplierCat,
			String format, String artist, String title,
			String shortDescription, String barcode, int onHand,
			float dealerPrice, String binNo, float price1, int outOfStock) {
		ProductId = productId;
		SuppCode = suppCode;
		SupplierCat = supplierCat;
		Format = format;
		Artist = artist;
		Title = title;
		ShortDescription = shortDescription;
		Barcode = barcode;
		OnHand = onHand;
		DealerPrice = dealerPrice;
		BinNo = binNo;
		Price1 = price1;
		OutOfStock = outOfStock;
	}

	public int getProductId() {
		return ProductId;
	}

	public void setProductId(int productId) {
		ProductId = productId;
	}

	public String getSuppCode() {
		return SuppCode;
	}

	public void setSuppCode(String suppCode) {
		SuppCode = suppCode;
	}

	public String getSupplierCat() {
		return SupplierCat;
	}

	public void setSupplierCat(String supplierCat) {
		SupplierCat = supplierCat;
	}

	public String getFormat() {
		return Format;
	}

	public void setFormat(String format) {
		Format = format;
	}

	public String getArtist() {
		return Artist;
	}

	public void setArtist(String artist) {
		Artist = artist;
	}

	public String getTitle() {
		return Title;
	}

	public void setTitle(String title) {
		Title = title;
	}

	public String getShortDescription() {
		return ShortDescription;
	}

	public void setShortDescription(String shortDescription) {
		ShortDescription = shortDescription;
	}

	public String getBarcode() {
		return Barcode;
	}

	public void setBarcode(String barcode) {
		Barcode = barcode;
	}

	public int getOnHand() {
		return OnHand;
	}

	public void setOnHand(int onHand) {
		OnHand = onHand;
	}

	public float getDealerPrice() {
		return DealerPrice;
	}

	public void setDealerPrice(float dealerPrice) {
		DealerPrice = dealerPrice;
	}

	public String getBinNo() {
		return BinNo;
	}

	public void setBinNo(String binNo) {
		BinNo = binNo;
	}

	public float getPrice1() {
		return Price1;
	}

	public void setPrice1(float price1) {
		Price1 = price1;
	}

	public int getOutOfStock() {
		return OutOfStock;
	}

	public void setOutOfStock(int outOfStock) {
		OutOfStock = outOfStock;
	}
	
}
