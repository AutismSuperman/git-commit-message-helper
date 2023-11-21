package com.fulinlin.ui.commit;

import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.JTextComponent;
import java.awt.*;

class CommitCaret extends DefaultCaret {

    public CommitCaret() {
        setBlinkRate(500); // 设置光标的闪烁速率为500毫秒
    }

    @Override
    protected synchronized void damage(Rectangle r) {
        if (r != null) {
            JTextComponent comp = getComponent();
            x = r.x;
            y = r.y;
            width = 2; // 设置光标的宽度
            height = r.height;
            comp.repaint();
        }
    }

    @Override
    public void paint(Graphics g) {
        JTextComponent comp = getComponent();
        if (comp == null)
            return;
        int dot = getDot();
        Rectangle r;
        try {
            r = comp.modelToView(dot);
            if (r == null)
                return;
        } catch (BadLocationException e) {
            return;
        }
        if ((x != r.x) || (y != r.y)) {
            repaint(); // 防止不完全擦除
            damage(r);
            return;
        }
        if (isVisible())
            g.fillRect(r.x, r.y, width, r.height); // 画一个矩形作为光标
    }

}