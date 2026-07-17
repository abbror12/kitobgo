package com.example.kitobgo.order.emu;

import com.example.kitobgo.order.Order;
import com.example.kitobgo.order.OrderItem;
import com.example.kitobgo.order.Region;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/**
 * EMU ga topshiriladigan pasilkalar ro'yxatini .xlsx qilib yozadi — admin shu faylni
 * yuklab olib pochtaga beradi. Trek-raqam ustuni ataylab bo'sh: uni EMU to'ldiradi,
 * keyin admin qaytarib tizimga kiritadi.
 */
@Component
public class EmuExcelWriter {

    private static final String[] HEADERS = {
            "№", "Pasilka nomi", "Mijoz", "Telefon", "Viloyat",
            "Tuman", "Mo'ljal", "Summa (so'm)", "Trek-raqam"
    };

    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** Buyurtmalar pasilka yozuvi biriktirilgan holda kelishi kutiladi (nom shundan olinadi). */
    public ExcelFile write(List<Order> orders) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("EMU");
            writeHeader(workbook, sheet);

            int rowIndex = 1;
            for (Order order : orders) {
                writeOrder(sheet.createRow(rowIndex), rowIndex, order);
                rowIndex++;
            }

            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ExcelFile("emu-" + LocalDate.now().format(FILE_DATE) + ".xlsx", out.toByteArray());
        } catch (IOException e) {
            throw new UncheckedIOException("EMU Excel faylini yozib bo'lmadi", e);
        }
    }

    private void writeHeader(Workbook workbook, Sheet sheet) {
        Font bold = workbook.createFont();
        bold.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(bold);

        Row header = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(style);
        }
    }

    private void writeOrder(Row row, int number, Order order) {
        EmuShipment shipment = order.getEmuShipment();
        Region region = order.getRegion();

        row.createCell(0).setCellValue(number);
        row.createCell(1).setCellValue(shipment != null ? shipment.getParcelName() : EmuParcelName.of(order));
        row.createCell(2).setCellValue(nullToEmpty(order.getCustomerName()));
        row.createCell(3).setCellValue(nullToEmpty(order.getCustomerPhone()));
        row.createCell(4).setCellValue(region != null ? region.getLabel() : "");
        row.createCell(5).setCellValue(nullToEmpty(order.getDistrict()));
        row.createCell(6).setCellValue(nullToEmpty(order.getLandmark()));
        row.createCell(7).setCellValue(totalPrice(order));
        row.createCell(8).setCellValue("");   // trek-raqamni EMU beradi
    }

    /** Buyurtma summasi — muzlatilgan narxlar bo'yicha (mijoz yetkazishda shuni to'laydi). */
    private int totalPrice(Order order) {
        int total = 0;
        for (OrderItem item : order.getItems()) {
            Integer price = item.getPriceAtPurchase();
            Integer quantity = item.getQuantity();
            if (price != null && quantity != null) {
                total += price * quantity;
            }
        }
        return total;
    }

    private String nullToEmpty(String value) {
        return Objects.requireNonNullElse(value, "");
    }
}
