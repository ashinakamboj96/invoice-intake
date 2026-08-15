package com.zamp.invoice.extraction;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.awt.Rectangle;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OcrWord {

    private String text;
    private float confidence;
    private Rectangle boundingBox;
}
