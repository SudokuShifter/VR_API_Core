package com.inlinegroup.vrcalculationbackend.exceptions;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
@Setter
@Slf4j
public class ErrorMsgDataObject {
    List<String> data;

    public ErrorMsgDataObject() {
        this.data = new ArrayList<>();
    }

    public void addMsg(String msg) {
        this.data.add(msg);
    }

    @Override
    public String toString() {
        Optional<String> msg = this.data.stream().reduce((s1, s2) -> s1 + " - " + s2);
        return msg.orElse("");
    }
}
