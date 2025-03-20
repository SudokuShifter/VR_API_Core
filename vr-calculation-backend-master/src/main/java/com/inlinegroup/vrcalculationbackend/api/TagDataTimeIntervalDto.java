package com.inlinegroup.vrcalculationbackend.api;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.LinkedList;

@Builder
@Setter
@Getter
public class TagDataTimeIntervalDto implements Comparable<TagDataTimeIntervalDto> {
    private String id;
    private String name;
    private LocalDateTime timeStart;
    private LocalDateTime timeEnd;
    private LinkedList<Double> values;
    private LinkedList<String> timestamps;

    @Override
    public boolean equals(Object obj) {
        if (obj == null){
            return false;
        }
        if(obj instanceof TagDataTimeIntervalDto tagData){
            return this.id.equals(tagData.id);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public int compareTo(TagDataTimeIntervalDto o) {
        return this.id.compareTo(o.id);
    }
}
