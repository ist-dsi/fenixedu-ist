/**
 * Copyright © 2013 Instituto Superior Técnico
 *
 * This file is part of FenixEdu IST QUC.
 *
 * FenixEdu IST QUC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu IST QUC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu IST QUC.  If not, see <http://www.gnu.org/licenses/>.
 */
/**
 * 
 */
package pt.ist.fenixedu.quc.ui.renderers;

import pt.ist.fenixWebFramework.renderers.InputRenderer;
import pt.ist.fenixWebFramework.renderers.components.HtmlComponent;
import pt.ist.fenixWebFramework.renderers.components.HtmlTextArea;
import pt.ist.fenixWebFramework.renderers.components.HtmlTextInput;
import pt.ist.fenixWebFramework.renderers.layouts.Layout;
import pt.ist.fenixWebFramework.renderers.utils.RenderUtils;

import com.google.common.base.Strings;

/**
 * @author - Ricardo Rodrigues (ricardo.rodrigues@ist.utl.pt)
 * 
 */
public class InquiryTextBoxQuestionRenderer extends InputRenderer {

    private Integer maxLength;
    private String defaultSize;

    @Override
    protected Layout getLayout(Object object, Class type) {

        return new Layout() {

            @Override
            public HtmlComponent createComponent(Object object, Class type) {

                Boolean textArea = (Boolean) getContext().getProperties().get("textArea");
                Boolean readOnly = (Boolean) getContext().getProperties().get("readOnly");

                if (textArea != null && textArea) {
                    HtmlTextArea htmlTextArea = new HtmlTextArea();
                    htmlTextArea.setRows(5);
                    htmlTextArea.setColumns(50);
                    String value = object != null ? object.toString() : null;
                    if (readOnly && Strings.isNullOrEmpty(value)) {
                        value = RenderUtils.getResourceString("INQUIRIES_RESOURCES", "label.inquiry.question.notAnswered");
                    }
                    htmlTextArea.setValue(value);
                    htmlTextArea.setReadOnly(readOnly);
                    return htmlTextArea;
                } else {
                    HtmlTextInput htmlTextInput = new HtmlTextInput();
                    htmlTextInput.setSize(getDefaultSize().toString());
                    htmlTextInput.setMaxLength(getMaxLength());
                    String value = object != null ? object.toString() : null;
                    if (readOnly && Strings.isNullOrEmpty(value)) {
                        value = RenderUtils.getResourceString("INQUIRIES_RESOURCES", "label.inquiry.question.notAnswered");
                    }
                    htmlTextInput.setValue(value);
                    htmlTextInput.setReadOnly(readOnly);
                    return htmlTextInput;
                }
            }
        };
    }

    public Integer getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(Integer maxLength) {
        this.maxLength = maxLength;
    }

    public void setDefaultSize(String defaultSize) {
        this.defaultSize = defaultSize;
    }

    public String getDefaultSize() {
        return defaultSize;
    }
}
