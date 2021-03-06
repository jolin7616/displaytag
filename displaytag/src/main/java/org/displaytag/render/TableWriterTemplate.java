/**
 * Copyright (C) 2002-2014 Fabrizio Giustina, the Displaytag team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.displaytag.render;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.jsp.JspException;

import org.apache.commons.lang3.ObjectUtils;
import org.displaytag.decorator.TableDecorator;
import org.displaytag.model.Column;
import org.displaytag.model.ColumnIterator;
import org.displaytag.model.HeaderCell;
import org.displaytag.model.Row;
import org.displaytag.model.RowIterator;
import org.displaytag.model.TableModel;
import org.displaytag.properties.MediaTypeEnum;
import org.displaytag.properties.TableProperties;
import org.displaytag.util.TagConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A template that encapsulates and drives the construction of a table based on a given table model and configuration.
 * This class is meant to be extended by classes that build tables sharing the same structure, sorting, and grouping,
 * but that write them in different formats and to various destinations. Subclasses must provide the format- and
 * destination-specific implementations of the abstract methods this class calls to build a table. (Background: This
 * class came about because our users wanted to export tables to Excel and PDF just as they were presented in HTML. It
 * originates with the TableTagData.writeHTMLData method, factoring its logic so that it can be re-used by classes that
 * write the tables as PDF, Excel, RTF and other formats. TableTagData.writeHTMLData now calls an HTML extension of this
 * class to write tables in HTML format to a JSP page.)
 * @author Jorge L. Barroso
 * @version $Id$
 */
public abstract class TableWriterTemplate
{

    /** The Constant GROUP_START. */
    public static final short GROUP_START = -2;

    /** The Constant GROUP_END. */
    public static final short GROUP_END = 5;

    /** The Constant GROUP_START_AND_END. */
    public static final short GROUP_START_AND_END = 3;

    /** The Constant GROUP_NO_CHANGE. */
    public static final short GROUP_NO_CHANGE = 0;

    /** The Constant NO_RESET_GROUP. */
    protected static final int NO_RESET_GROUP = 42000;

    /**
     * logger.
     */
    private static Logger log = LoggerFactory.getLogger(TableWriterTemplate.class);

    /**
     * Table unique id.
     */
    private String id;

    /** The lowest ended group. */
    int lowestEndedGroup;

    /** The lowest started group. */
    int lowestStartedGroup;

    /**
     * Given a table model, this method creates a table, sorting and grouping it per its configuration, while delegating
     * where and how it writes the table to subclass objects. (Background: This method refactors
     * TableTagData.writeHTMLData method. See above.)
     * @param model The table model used to build the table.
     * @param id This table's page id.
     * @throws JspException if any exception thrown while constructing the tablei, it is caught and rethrown as a
     * JspException. Extension classes may throw all sorts of exceptions, depending on their respective formats and
     * destinations.
     */
    public void writeTable(TableModel model, String id) throws JspException
    {
        try
        {
            // table id used for logging
            this.id = id;

            TableProperties properties = model.getProperties();

            if (log.isDebugEnabled())
            {
                log.debug("[" + this.id + "] writeTable called for table [" + this.id + "]");
            }

            // Handle empty table
            boolean noItems = model.getRowListPage().isEmpty();
            if (noItems && !properties.getEmptyListShowTable())
            {
                writeEmptyListMessage(properties.getEmptyListMessage());
                return;
            }

            // search result, navigation bar and export links.
            writeTopBanner(model);

            // open table
            writeTableOpener(model);

            // render caption
            if (model.getCaption() != null)
            {
                writeCaption(model);
            }

            // render headers
            if (model.getProperties().getShowHeader())
            {
                writeTableHeader(model);
            }

            // render footer prior to body
            if (model.getFooter() != null)
            {
                writePreBodyFooter(model);
            }

            // open table body
            writeTableBodyOpener(model);

            // render table body
            writeTableBody(model);

            // close table body
            writeTableBodyCloser(model);

            // render footer after body
            if (model.getFooter() != null)
            {
                writePostBodyFooter(model);
            }

            // close table
            writeTableCloser(model);

            if (model.getTableDecorator() != null)
            {
                writeDecoratedTableFinish(model);
            }

            writeBottomBanner(model);

            if (log.isDebugEnabled())
            {
                log.debug("[" + this.id + "] writeTable end");
            }
        }
        catch (Exception e)
        {
            throw new JspException(e);
        }
    }

    /**
     * Called by writeTable to write a message explaining that the table model contains no data.
     * @param emptyListMessage A message explaining that the table model contains no data.
     * @throws Exception if it encounters an error while writing.
     */
    protected abstract void writeEmptyListMessage(String emptyListMessage) throws Exception;

    /**
     * Called by writeTable to write a summary of the search result this table reports and the table's pagination
     * interface.
     * @param model The table model for which the banner is written.
     * @throws Exception if it encounters an error while writing.
     */
    protected abstract void writeTopBanner(TableModel model) throws Exception;

    /**
     * Called by writeTable to write the start of the table structure.
     * @param model The table model for which the content is written.
     * @throws Exception if it encounters an error while writing.
     */
    protected abstract void writeTableOpener(TableModel model) throws Exception;

    /**
     * Called by writeTable to write the table's caption.
     * @param model The table model for which the content is written.
     * @throws Exception if it encounters an error while writing.
     */
    protected abstract void writeCaption(TableModel model) throws Exception;

    /**
     * Called by writeTable to write the table's header columns.
     * @param model The table model for which the content is written.
     * @throws Exception if it encounters an error while writing.
     */
    protected abstract void writeTableHeader(TableModel model) throws Exception;

    /**
     * Called by writeTable to write table footer before table body.
     * @param model The table model for which the content is written.
     * @throws Exception if it encounters an error while writing.
     */
    protected abstract void writePreBodyFooter(TableModel model) throws Exception;

    /**
     * Called by writeTable to write the start of the table's body.
     * @param model The table model for which the content is written.
     * @throws Exception if it encounters an error while writing.
     */
    protected abstract void writeTableBodyOpener(TableModel model) throws Exception;

    // protected abstract void writeTableBody(TableModel model);

    /**
     * Called by writeTable to write the end of the table's body.
     * @param model The table model for which the content is written.
     * @throws Exception if it encounters an error while writing.
     */
    protected abstract void writeTableBodyCloser(TableModel model) throws Exception;

    /**
     * Called by writeTable to write table footer after table body.
     * @param model The table model for which the content is written.
     * @throws Exception if it encounters an error while writing.
     */
    protected abstract void writePostBodyFooter(TableModel model) throws Exception;

    /**
     * Called by writeTable to write the end of the table's structure.
     * @param model The table model for which the content is written.
     * @throws Exception if it encounters an error while writing.
     */
    protected abstract void writeTableCloser(TableModel model) throws Exception;

    /**
     * Called by writeTable to decorate the table.
     * @param model The table model for which the content is written.
     * @throws Exception if it encounters an error while writing.
     */
    protected abstract void writeDecoratedTableFinish(TableModel model) throws Exception;

    /**
     * Called by writeTable to write the table's footer.
     * @param model The table model for which the content is written.
     * @throws Exception if it encounters an error while writing.
     */
    protected abstract void writeBottomBanner(TableModel model) throws Exception;

    /**
     * Given a table model, writes the table body content, sorting and grouping it per its configuration, while
     * delegating where and how it writes to subclass objects. (Background: This method refactors
     * TableTagData.writeTableBody method. See above.)
     * @param model The table model used to build the table body.
     * @throws Exception if an error is encountered while writing the table body.
     */
    protected void writeTableBody(TableModel model) throws Exception
    {
        // Ok, start bouncing through our list (only the visible part)
        boolean fullList = false;
        if (!MediaTypeEnum.HTML.equals(model.getMedia()) && model.getProperties().getExportFullList())
        {
            fullList = true;
        }
        RowIterator rowIterator = model.getRowIterator(fullList);
        TableTotaler totalsTableDecorator = model.getTotaler();
        if (totalsTableDecorator == null)
        {
            totalsTableDecorator = TableTotaler.NULL;
        }

        // iterator on rows
        TableDecorator tableDecorator = model.getTableDecorator();
        Row previousRow = null;
        Row currentRow = null;
        Row nextRow = null;
        Map<Integer, CellStruct> previousRowValues = new HashMap<Integer, CellStruct>(10);
        Map<Integer, CellStruct> currentRowValues = new HashMap<Integer, CellStruct>(10);
        Map<Integer, CellStruct> nextRowValues = new HashMap<Integer, CellStruct>(10);

        while (nextRow != null || rowIterator.hasNext())
        {
            // The first pass
            if (currentRow == null)
            {
                currentRow = rowIterator.next();
            }
            else
            {
                previousRow = currentRow;
                currentRow = nextRow;
            }

            if (previousRow != null)
            {
                previousRowValues.putAll(currentRowValues);
            }
            if (!nextRowValues.isEmpty())
            {
                currentRowValues.putAll(nextRowValues);
            }
            // handle the first pass
            else
            {
                ColumnIterator columnIterator = currentRow.getColumnIterator(model.getHeaderCellList());
                // iterator on columns
                if (log.isDebugEnabled())
                {
                    log.debug(" creating ColumnIterator on " + model.getHeaderCellList());
                }
                while (columnIterator.hasNext())
                {
                    Column column = columnIterator.nextColumn();

                    // Get the value to be displayed for the column
                    column.initialize();

                    @SuppressWarnings("deprecation")
                    String cellvalue = MediaTypeEnum.HTML.equals(model.getMedia())
                        ? column.getChoppedAndLinkedValue()
                        : ObjectUtils.toString(column.getValue(true));

                    CellStruct struct = new CellStruct(column, cellvalue);
                    currentRowValues.put(new Integer(column.getHeaderCell().getColumnNumber()), struct);
                }
            }

            nextRowValues.clear();
            // Populate the next row values
            nextRow = rowIterator.hasNext() ? rowIterator.next() : null;
            if (nextRow != null)
            {
                ColumnIterator columnIterator = nextRow.getColumnIterator(model.getHeaderCellList());
                // iterator on columns
                if (log.isDebugEnabled())
                {
                    log.debug(" creating ColumnIterator on " + model.getHeaderCellList());
                }
                while (columnIterator.hasNext())
                {
                    Column column = columnIterator.nextColumn();
                    column.initialize();
                    // Get the value to be displayed for the column

                    @SuppressWarnings("deprecation")
                    String cellvalue = MediaTypeEnum.HTML.equals(model.getMedia())
                        ? column.getChoppedAndLinkedValue()
                        : ObjectUtils.toString(column.getValue(true));

                    CellStruct struct = new CellStruct(column, cellvalue);
                    nextRowValues.put(new Integer(column.getHeaderCell().getColumnNumber()), struct);
                }
            }
            // now we are going to create the current row; reset the decorator to the current row
            if (tableDecorator != null)
            {
                tableDecorator.initRow(currentRow.getObject(), currentRow.getRowNumber(), currentRow.getRowNumber()
                    + rowIterator.getPageOffset());
            }
            if (totalsTableDecorator != null)
            {
                totalsTableDecorator.initRow(
                    currentRow.getRowNumber(),
                    currentRow.getRowNumber() + rowIterator.getPageOffset());
            }

            ArrayList<CellStruct> structsForRow = new ArrayList<CellStruct>(model.getHeaderCellList().size());
            this.lowestEndedGroup = NO_RESET_GROUP;
            this.lowestStartedGroup = NO_RESET_GROUP;

            for (HeaderCell header : model.getHeaderCellList())
            {

                // Get the value to be displayed for the column
                CellStruct struct = currentRowValues.get(new Integer(header.getColumnNumber()));
                struct.decoratedValue = struct.bodyValue;
                // Check and see if there is a grouping transition. If there is, then notify the decorator
                if (header.getGroup() != -1)
                {
                    CellStruct prior = previousRowValues.get(new Integer(header.getColumnNumber()));
                    CellStruct next = nextRowValues.get(new Integer(header.getColumnNumber()));
                    // Why npe?
                    String priorBodyValue = prior != null ? prior.bodyValue : null;
                    String nextBodyValue = next != null ? next.bodyValue : null;
                    short groupingValue = groupColumns(
                        struct.bodyValue,
                        priorBodyValue,
                        nextBodyValue,
                        header.getGroup());

                    if (tableDecorator != null || totalsTableDecorator != null)
                    {
                        switch (groupingValue)
                        {
                            case GROUP_START :
                                totalsTableDecorator.startGroup(struct.bodyValue, header.getGroup());
                                if (tableDecorator != null)
                                {
                                    tableDecorator.startOfGroup(struct.bodyValue, header.getGroup());
                                }
                                break;
                            case GROUP_END :
                                totalsTableDecorator.stopGroup(struct.bodyValue, header.getGroup());
                                if (tableDecorator != null)
                                {
                                    tableDecorator.endOfGroup(struct.bodyValue, header.getGroup());
                                }
                                break;
                            case GROUP_START_AND_END :
                                totalsTableDecorator.startGroup(struct.bodyValue, header.getGroup());
                                if (tableDecorator != null)
                                {
                                    tableDecorator.startOfGroup(struct.bodyValue, header.getGroup());
                                }
                                totalsTableDecorator.stopGroup(struct.bodyValue, header.getGroup());
                                if (tableDecorator != null)
                                {
                                    tableDecorator.endOfGroup(struct.bodyValue, header.getGroup());
                                }

                                break;
                            default :
                                break;
                        }
                    }
                    if (tableDecorator != null)
                    {
                        struct.decoratedValue = tableDecorator.displayGroupedValue(
                            struct.bodyValue,
                            groupingValue,
                            header.getColumnNumber());
                    }
                    else if (groupingValue == GROUP_END || groupingValue == GROUP_NO_CHANGE)
                    {
                        struct.decoratedValue = TagConstants.EMPTY_STRING;
                    }
                }
                structsForRow.add(struct);
            }

            if (totalsTableDecorator != null)
            {
                writeSubgroupStart(model);
            }
            if (tableDecorator != null)
            {
                writeDecoratedRowStart(model);
            }
            // open row
            writeRowOpener(currentRow);

            for (CellStruct struct : structsForRow)
            {
                writeColumnOpener(struct.column);
                writeColumnValue(struct.decoratedValue, struct.column);
                writeColumnCloser(struct.column);
            }

            if (model.isEmpty())
            {
                if (log.isDebugEnabled())
                {
                    log.debug("[" + this.id + "] table has no columns");
                }
                // render empty row
                writeRowWithNoColumns(currentRow.getObject().toString());
            }

            // close row
            writeRowCloser(currentRow);
            // decorate row finish
            if (model.getTableDecorator() != null)
            {
                writeDecoratedRowFinish(model);
            }
            if (model.getTotaler() != null)
            {
                writeSubgroupStop(model);
            }
        }
        // how is this really going to work?
        // the totaler is notified whenever we start or stop a group, and the totaler tracks the current state of the
        // the totals; the totaler writes nothing
        // when the row is finished, it is the responsibility of the decorator or exporter to ask for the totaler total
        // and write it when the row is finished,

        // render empty list message
        if (model.getRowListPage().isEmpty())
        {
            writeEmptyListRowMessage(new MessageFormat(model.getProperties().getEmptyListRowMessage(), model
                .getProperties()
                .getLocale()).format(new Object[]{new Integer(model.getNumberOfColumns())}));
        }
    }

    /*
     * writeTableBody callback methods
     */

    /**
     * Called by writeTableBody to write to decorate the table.
     * @param model The table model for which the content is written.
     * @throws Exception if it encounters an error while writing.
     */
    protected abstract void writeDecoratedRowStart(TableModel model) throws Exception;

    /**
     * Write subgroup start.
     *
     * @param model the model
     * @throws Exception the exception
     */
    protected void writeSubgroupStart(TableModel model) throws Exception
    {
    }

    /**
     * Write subgroup stop.
     *
     * @param model the model
     * @throws Exception the exception
     */
    protected void writeSubgroupStop(TableModel model) throws Exception
    {
    }

    /**
     * Called by writeTableBody to write the start of the row structure.
     * @param row The table row for which the content is written.
     * @throws Exception if it encounters an error while writing.
     */
    protected abstract void writeRowOpener(Row row) throws Exception;

    /**
     * Called by writeTableBody to write the start of the column structure.
     * @param column The table column for which the content is written.
     * @throws Exception if it encounters an error while writing.
     */
    protected abstract void writeColumnOpener(Column column) throws Exception;

    /**
     * Called by writeTableBody to write a column's value.
     * @param value The column value.
     * @param column The table column for which the content is written.
     * @throws Exception if it encounters an error while writing.
     */
    protected abstract void writeColumnValue(Object value, Column column) throws Exception;

    /**
     * Called by writeTableBody to write the end of the column structure.
     * @param column The table column for which the content is written.
     * @throws Exception if it encounters an error while writing.
     */
    protected abstract void writeColumnCloser(Column column) throws Exception;

    /**
     * Called by writeTableBody to write a row that has no columns.
     * @param value The row value.
     * @throws Exception if it encounters an error while writing.
     */
    protected abstract void writeRowWithNoColumns(String value) throws Exception;

    /**
     * Called by writeTableBody to write the end of the row structure.
     * @param row The table row for which the content is written.
     * @throws Exception if it encounters an error while writing.
     */
    protected abstract void writeRowCloser(Row row) throws Exception;

    /**
     * Called by writeTableBody to decorate the table.
     * @param model The table model for which the content is written.
     * @throws Exception if it encounters an error while writing.
     */
    protected abstract void writeDecoratedRowFinish(TableModel model) throws Exception;

    /**
     * Called by writeTableBody to write a message explaining that the row contains no data.
     * @param message The message explaining that the row contains no data.
     * @throws Exception if it encounters an error while writing.
     */
    protected abstract void writeEmptyListRowMessage(String message) throws Exception;

    /**
     * This takes a column value and grouping index as the argument. It then groups the column and returns the
     * appropriate string back to the caller.
     *
     * @param value String current cell value
     * @param previous the previous
     * @param next the next
     * @param currentGroup the current group
     * @return String
     */
    @SuppressWarnings("deprecation")
    protected short groupColumns(String value, String previous, String next, int currentGroup)
    {

        short groupingKey = GROUP_NO_CHANGE;
        if (this.lowestEndedGroup < currentGroup)
        {
            // if a lower group has ended, cascade so that all subgroups end as well
            groupingKey += GROUP_END;
        }
        else if (next == null || !ObjectUtils.equals(value, next))
        {
            // at the end of the list
            groupingKey += GROUP_END;
            this.lowestEndedGroup = currentGroup;
        }

        if (this.lowestStartedGroup < currentGroup)
        {
            // if a lower group has started, cascade so that all subgroups restart as well
            groupingKey += GROUP_START;
        }
        else if (previous == null || !ObjectUtils.equals(value, previous))
        {
            // At the start of the list
            groupingKey += GROUP_START;
            this.lowestStartedGroup = currentGroup;
        }
        return groupingKey;
    }

    /**
     * The Class CellStruct.
     */
    static class CellStruct
    {

        /** The column. */
        Column column;

        /** The body value. */
        String bodyValue;

        /** The decorated value. */
        String decoratedValue;

        /**
         * Instantiates a new cell struct.
         *
         * @param theColumn the the column
         * @param bodyValueParam the body value param
         */
        public CellStruct(Column theColumn, String bodyValueParam)
        {
            this.column = theColumn;
            this.bodyValue = bodyValueParam;
        }
    }
}
