package com.zh.zhaicodempther.controller;

import com.zh.zhaicodempther.common.PageQuery;
import com.zh.zhaicodempther.common.PageResult;
import com.zh.zhaicodempther.common.Result;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;

@RestController
@RequestMapping("/health")
public class HealthController {


    @GetMapping("/check")
    private Result<String> check() {
        ArrayList<String> list = new ArrayList<>();
        list.add("ok");
        return Result.ok("ok");
    }

    @GetMapping("/page")
    private Result<PageResult<String>> page(@ParameterObject PageQuery query) {
        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add("ok");
        arrayList.add("CC");
        arrayList.add("DD");
        arrayList.add("EE");
        return Result.ok(PageResult.of(arrayList, arrayList.size(), query));
    }
}
