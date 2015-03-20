/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST Parking.
 *
 * FenixEdu IST Parking is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST Parking is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST Parking.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ist.fenixedu.parking.dto;

import java.io.Serializable;

import pt.ist.fenixedu.parking.domain.ParkingParty;
import pt.ist.fenixedu.parking.domain.Vehicle;

public class VehicleBean implements Serializable {

    private Vehicle vehicle;

    private ParkingParty parkingParty;

    private String vehiclePlateNumber;

    private String vehicleMake;

    private Boolean deleteVehicle;

    public VehicleBean(Vehicle vehicle, ParkingParty parkingParty) {
        setVehicle(vehicle);
        setParkingParty(parkingParty);
        setVehiclePlateNumber(vehicle.getPlateNumber());
        setVehicleMake(vehicle.getVehicleMake());
        setDeleteVehicle(false);
    }

    public VehicleBean(ParkingParty parkingParty) {
        setParkingParty(parkingParty);
        setDeleteVehicle(false);
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        if (vehicle != null) {
            this.vehicle = vehicle;
        } else {
            this.vehicle = null;
        }
    }

    public ParkingParty getParkingParty() {
        return parkingParty;
    }

    public void setParkingParty(ParkingParty parkingParty) {
        if (parkingParty != null) {
            this.parkingParty = parkingParty;
        } else {
            this.parkingParty = null;
        }
    }

    public String getVehicleMake() {
        return vehicleMake;
    }

    public void setVehicleMake(String vehicleMake) {
        this.vehicleMake = vehicleMake;
    }

    public String getVehiclePlateNumber() {
        return vehiclePlateNumber;
    }

    public void setVehiclePlateNumber(String vehiclePlateNumber) {
        this.vehiclePlateNumber = vehiclePlateNumber;
    }

    public Boolean getDeleteVehicle() {
        return deleteVehicle;
    }

    public void setDeleteVehicle(Boolean deleteVehicle) {
        this.deleteVehicle = deleteVehicle;
    }
}
