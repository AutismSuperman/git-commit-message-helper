package com.fulinlin.ui.commit;

import com.intellij.ide.ui.laf.darcula.ui.DarculaTextBorder;
import com.intellij.ide.ui.laf.darcula.ui.TextFieldWithPopupHandlerUI;
import com.intellij.util.ui.JBInsets;
import com.intellij.util.ui.MacUIUtil;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

import static com.intellij.ide.ui.laf.darcula.DarculaUIUtil.*;

public class CommitDarculaTextBorder extends DarculaTextBorder {

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        if (((JComponent) c).getClientProperty("JTextField.Search.noBorderRing") == Boolean.TRUE) return;

        Rectangle r = new Rectangle(x, y, width, height);
        boolean focused = isFocused(c);

        if (TextFieldWithPopupHandlerUI.isSearchField(c)) {
            paintSearchArea((Graphics2D) g, r, (JTextComponent) c, false);
        } else if (isTableCellEditor(c)) {
            paintCellEditorBorder((Graphics2D) g, c, r, focused);
        } else if (!(c.getParent() instanceof JComboBox)) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                        MacUIUtil.USE_QUARTZ ? RenderingHints.VALUE_STROKE_PURE : RenderingHints.VALUE_STROKE_NORMALIZE);

                JBInsets.removeFrom(r, paddings());
                g2.translate(r.x, r.y);

                float lw = lw(g2);
                float bw = bw();

                clipForBorder(c, g2, r.width, r.height);
                /*
                Object op = ((JComponent) c).getClientProperty("JComponent.outline");
                if (c.isEnabled() && op != null) {
                    paintOutlineBorder(g2, r.width, r.height, 0, isSymmetric(), focused, Outline.valueOf(op.toString()));
                } else {
                    Path2D border = new Path2D.Float(Path2D.WIND_EVEN_ODD);
                    border.append(new Rectangle2D.Float(bw, bw, r.width - bw * 2, r.height - bw * 2), false);
                    border.append(new Rectangle2D.Float(bw + lw, bw + lw, r.width - (bw + lw) * 2, r.height - (bw + lw) * 2), false);
                    boolean editable = !(c instanceof JTextComponent) || ((JTextComponent) c).isEditable();
                    g2.setColor(getOutlineColor(c.isEnabled() && editable, focused));
                    g2.fill(border);
                }
                */
                Path2D border = new Path2D.Float(Path2D.WIND_EVEN_ODD);
                border.append(new Rectangle2D.Float(bw, bw, r.width - bw * 2, r.height - bw * 2), false);
                border.append(new Rectangle2D.Float(bw + lw, bw + lw, r.width - (bw + lw) * 2, r.height - (bw + lw) * 2), false);
                g2.setColor(getOutlineColor(false, focused));
                g2.fill(border);
            } finally {
                g2.dispose();
            }
        }
    }
}
