package com.rdns.rohost;

import javax.validation.constraints.NotNull;

public class MyReqData {
	@NotNull
	private String recordName;
	@NotNull
	private String ptrDomainName;
	
	public String getRecordName() {
		return recordName;
	}
	public void setRecordName(String recordName) {
		this.recordName = recordName;
	}
	public String getPtrDomainName() {
		return ptrDomainName;
	}
	public void setPtrDomainName(String ptrDomainName) {
		this.ptrDomainName = ptrDomainName;
	}
}
