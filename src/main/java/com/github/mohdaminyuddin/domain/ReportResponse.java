package com.github.mohdaminyuddin.domain;

import java.util.List;

public class ReportResponse {
	
	private List<Data> data;
	private String last_updated_at;
	private About about;
	
	public List<Data> getData() {
		return data;
	}
	public void setData(List<Data> data) {
		this.data = data;
	}
	public String getLast_updated_at() {
		return last_updated_at;
	}
	public void setLast_updated_at(String last_updated_at) {
		this.last_updated_at = last_updated_at;
	}
	public About getAbout() {
		return about;
	}
	public void setAbout(About about) {
		this.about = about;
	}
}
