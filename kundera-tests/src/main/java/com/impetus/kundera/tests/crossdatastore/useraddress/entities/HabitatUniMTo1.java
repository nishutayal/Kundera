package com.impetus.kundera.tests.crossdatastore.useraddress.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="ADDRESS", schema="KunderaTests")
public class HabitatUniMTo1 {
	@Id    
    @Column(name = "ADDRESS_ID")
    private String addressId;   
   

    @Column(name = "STREET")
    private String street;  
    
    	

	public String getAddressId() {
		return addressId;
	}

	public void setAddressId(String addressId) {
		this.addressId = addressId;
	}

	public String getStreet()
    {
        return street;
    }

    public void setStreet(String street)
    {
        this.street = street;
    }
}
