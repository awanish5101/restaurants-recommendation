package com.dtdl.restaurant.model;

import lombok.Data;
import java.util.List;

@Data
class Hour {
    private String dayOfWeek;
    private List<DayHour> dayHours;
    private boolean openForBusiness;
}
