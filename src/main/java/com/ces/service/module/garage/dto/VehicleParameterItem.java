package com.ces.service.module.garage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One free-form "ad: dəyər" row on a vehicle's technical parameters (e.g. "Mühərrik gücü" /
 * "150 HP"). No fixed schema — unlike Inventory's category-driven attributes, Qaraj equipment
 * types vary too widely to share one field set, so this is just a name/value pair the user types.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleParameterItem {

    private String name;

    private String value;
}
