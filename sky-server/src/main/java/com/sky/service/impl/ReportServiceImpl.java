package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WorkspaceService workspaceService;


    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        //存放时间表
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);

        while (!begin.equals(end)) {
            //计算有多少天
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {

            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Double turnover = orderMapper.getAmountByDate(beginTime, endTime);
            turnover = turnover == null ? 0.0 : turnover;
            turnoverList.add(turnover);
        }


        return TurnoverReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();
    }

    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);

        while (!begin.equals(end)) {
            //计算有多少天
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        List<Integer> newUserList = new ArrayList<>();
        List<Integer> totalUserList = new ArrayList<>();

        for (LocalDate date : dateList) {

            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map map = new HashMap();

            map.put("end", endTime);

            Integer totalUser = userMapper.countByMap(map);

            map.put("begin", beginTime);

            Integer newUser = userMapper.countByMap(map);

            newUserList.add(newUser);
            totalUserList.add(totalUser);
        }


        return UserReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .build();
    }

    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {

        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);

        while (!begin.equals(end)) {
            //计算有多少天
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            //每天的订单数
            Integer orderCount = getOrderCount(beginTime, endTime, null);
            orderCountList.add(orderCount);
            //每天的有效订单数
            Integer validCount = getOrderCount(beginTime, endTime, Orders.COMPLETED);
            validOrderCountList.add(validCount);
        }

        //计算时间区间内的订单总数
        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).get();
        //计算时间区间内的有效订单总数
        Integer validTotalOrderCount = validOrderCountList.stream().reduce(Integer::sum).get();

        Double orderCompletionRate = 0.0;
        if (totalOrderCount != 0) {
            orderCompletionRate = validTotalOrderCount.doubleValue() / totalOrderCount;
        }
        return OrderReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCompletionRate(orderCompletionRate)
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCount(validTotalOrderCount)
                .totalOrderCount(totalOrderCount)
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .build();
    }

    @Override
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop10(beginTime, endTime);

        List<String> names = salesTop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());

        List<Integer> numberList = salesTop10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());

        return SalesTop10ReportVO
                .builder()
                .nameList(StringUtils.join(names, ","))
                .numberList(StringUtils.join(numberList, ","))
                .build();
    }

    @Override
    public void exportBusinessData(HttpServletResponse response) {

        LocalDate dateBegin = LocalDate.now().minusDays(30);
        LocalDate dataEnd = LocalDate.now().minusDays(1);

        LocalDateTime begin = LocalDateTime.of(dateBegin, LocalTime.MIN);
        LocalDateTime end = LocalDateTime.of(dataEnd, LocalTime.MAX);

        BusinessDataVO businessData = workspaceService.getBusinessData(begin, end);

        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("/template/运营数据报表模板.xlsx");

        try {
            XSSFWorkbook excel = new XSSFWorkbook(inputStream);

            //填充数据
            XSSFSheet sheet1 = excel.getSheet("Sheet1");
            XSSFCell cell = sheet1.getRow(1).getCell(1);
            cell.setCellValue("时间：" + dateBegin + "至" + dataEnd);

            XSSFRow sheet1Row = sheet1.getRow(3);
            sheet1Row.getCell(2).setCellValue(businessData.getTurnover());
            sheet1Row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
            sheet1Row.getCell(6).setCellValue(businessData.getNewUsers());

            sheet1Row = sheet1.getRow(4);
            sheet1Row.getCell(2).setCellValue(businessData.getValidOrderCount());
            sheet1Row.getCell(4).setCellValue(businessData.getUnitPrice());


            for (int i = 0; i < 30; i++) {
                LocalDate date = dateBegin.plusDays(i);
                BusinessDataVO businessDataVO = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));

                sheet1Row = sheet1.getRow(7 + i);
                sheet1Row.getCell(1).setCellValue(date.toString());
                sheet1Row.getCell(2).setCellValue(businessDataVO.getTurnover());
                sheet1Row.getCell(3).setCellValue(businessDataVO.getValidOrderCount());
                sheet1Row.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
                sheet1Row.getCell(5).setCellValue(businessDataVO.getUnitPrice());
                sheet1Row.getCell(6).setCellValue(businessDataVO.getNewUsers());

            }


            ServletOutputStream outputStream = response.getOutputStream();
            excel.write(outputStream);

            outputStream.close();
            excel.close();

        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private Integer getOrderCount(LocalDateTime begin, LocalDateTime end, Integer status) {
        Map map = new HashMap<>();
        map.put("begin", begin);
        map.put("end", end);
        map.put("status", status);
        return orderMapper.countByMap(map);
    }

}
