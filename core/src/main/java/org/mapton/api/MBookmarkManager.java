/*
 * Copyright 2019 Patrik Karlström.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mapton.api;

import com.healthmarketscience.sqlbuilder.BinaryCondition;
import com.healthmarketscience.sqlbuilder.ComboCondition;
import com.healthmarketscience.sqlbuilder.InsertQuery;
import com.healthmarketscience.sqlbuilder.OrderObject;
import com.healthmarketscience.sqlbuilder.SelectQuery;
import com.healthmarketscience.sqlbuilder.UpdateQuery;
import com.healthmarketscience.sqlbuilder.custom.postgresql.PgBinaryCondition;
import com.healthmarketscience.sqlbuilder.dbspec.Constraint;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbConstraint;
import java.awt.Dimension;
import java.awt.Point;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.TreeSet;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javax.swing.SwingUtilities;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.mapton.core.db.DbBaseManager;
import org.mapton.core.ui.bookmark.BookmarkCategoryPanel;
import org.mapton.core.ui.bookmark.BookmarkColorPanel;
import org.mapton.core.ui.bookmark.BookmarkPanel;
import org.mapton.core.ui.bookmark.BookmarkZoomPanel;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import se.trixon.almond.nbp.Almond;
import se.trixon.almond.nbp.dialogs.NbMessage;
import se.trixon.almond.util.Dict;
import se.trixon.almond.util.fx.FxActionSwing;

/**
 *
 * @author Patrik Karlström
 */
public class MBookmarkManager extends DbBaseManager {

    public static final String COL_CATEGORY = "category";
    public static final String COL_COLOR = "color";
    public static final String COL_CREATED = "created";
    public static final String COL_DESCRIPTION = "description";
    public static final String COL_DISPLAY_MARKER = "display_marker";
    public static final String COL_ID = "bookmark_id";
    public static final String COL_LATITUDE = "latitude";
    public static final String COL_LONGITUDE = "longitude";
    public static final String COL_MODIFIED = "modified";
    public static final String COL_NAME = "name";
    public static final String COL_ZOOM = "zoom";
    private final ResourceBundle mBundle = NbBundle.getBundle(MBookmarkManager.class);

    private final DbColumn mCategory;
    private final DbColumn mColor;
    private final Columns mColumns = new Columns();
    private final DbColumn mDescription;
    private final DbColumn mDisplayMarker;
    private ObjectProperty<ObservableList<MBookmark>> mItems = new SimpleObjectProperty<>();
    private final DbColumn mLatitude;
    private final DbColumn mLongitude;
    private final DbColumn mName;
    private String mStoredFilter = "";
    private final DbColumn mTimeCreated;
    private final DbColumn mTimeModified;
    private final DbColumn mZoom;

    public static MBookmarkManager getInstance() {
        return Holder.INSTANCE;
    }

    private MBookmarkManager() {
        mTable = getSchema().addTable("bookmark");

        mId = mTable.addColumn(COL_ID, SQL_IDENTITY, null);
        mName = mTable.addColumn(COL_NAME, SQL_VARCHAR, Integer.MAX_VALUE);
        mCategory = mTable.addColumn(COL_CATEGORY, SQL_VARCHAR, Integer.MAX_VALUE);
        mDescription = mTable.addColumn(COL_DESCRIPTION, SQL_VARCHAR, Integer.MAX_VALUE);
        mColor = mTable.addColumn(COL_COLOR, SQL_VARCHAR, Integer.MAX_VALUE);
        mDisplayMarker = mTable.addColumn(COL_DISPLAY_MARKER, SQL_BOOLEAN, null);
        mLatitude = mTable.addColumn(COL_LATITUDE, SQL_DOUBLE, null);
        mLongitude = mTable.addColumn(COL_LONGITUDE, SQL_DOUBLE, null);
        mZoom = mTable.addColumn(COL_ZOOM, SQL_DOUBLE, null);
        mTimeCreated = mTable.addColumn(COL_CREATED, SQL_TIMESTAMP, null);
        mTimeModified = mTable.addColumn(COL_MODIFIED, SQL_TIMESTAMP, null);

        addNotNullConstraints(mName, mCategory, mDescription);
        create();
        mItems.setValue(FXCollections.observableArrayList());
        addMissingColumns();
        dbLoad();
    }

    public Columns columns() {
        return mColumns;
    }

    @Override
    public void create() {
        String indexName = getIndexName(new DbColumn[]{mId}, "pkey");
        DbConstraint primaryKeyConstraint = new DbConstraint(mTable, indexName, Constraint.Type.PRIMARY_KEY, mId);

        indexName = getIndexName(new DbColumn[]{mName, mCategory}, "key");
        DbConstraint uniqueKeyConstraint = new DbConstraint(mTable, indexName, Constraint.Type.UNIQUE, mName, mCategory);

        mDb.create(mTable, false, primaryKeyConstraint, uniqueKeyConstraint);
    }

    public synchronized void dbDelete(MBookmark bookmark) throws ClassNotFoundException, SQLException {
        mDb.delete(mTable, mId, bookmark.getId());
        dbLoad();
    }

    public synchronized void dbDelete(String category) throws ClassNotFoundException, SQLException {
        for (MBookmark bookmark : mItems.get()) {
            if (StringUtils.startsWith(bookmark.getCategory(), category)) {
                mDb.delete(mTable, mId, bookmark.getId());
            }
        }

        dbLoad();
    }

    public synchronized void dbDelete() throws ClassNotFoundException, SQLException {
        for (MBookmark bookmark : mItems.get()) {
            mDb.delete(mTable, mId, bookmark.getId());
        }

        dbLoad();
    }

    public synchronized void dbInsert(MBookmark bookmark) throws ClassNotFoundException, SQLException {
        dbInsertSilent(bookmark);
        dbLoad();
    }

    public synchronized Point dbInsert(ArrayList<MBookmark> bookmarks) {
        int imports = 0;
        int errors = 0;

        for (MBookmark bookmark : bookmarks) {
            try {
                dbInsertSilent(bookmark);
                imports++;
            } catch (ClassNotFoundException | SQLException ex) {
                errors++;
            }
        }

        Platform.runLater(() -> {
            dbLoad();
        });

        return new Point(imports, errors);
    }

    public synchronized ArrayList<MBookmark> dbLoad() {
        return dbLoad(mStoredFilter, true);
    }

    public synchronized ArrayList<MBookmark> dbLoad(String filter, boolean addToList) {
        if (mSelectPreparedStatement == null) {
            mSelectPlaceHolders.init(
                    mCategory,
                    mName,
                    mDescription
            );

            ComboCondition comboCondition = ComboCondition.or(
                    PgBinaryCondition.iLike(mCategory, mSelectPlaceHolders.get(mCategory)),
                    PgBinaryCondition.iLike(mDescription, mSelectPlaceHolders.get(mDescription)),
                    PgBinaryCondition.iLike(mName, mSelectPlaceHolders.get(mName))
            );

            SelectQuery selectQuery = new SelectQuery()
                    .addAllTableColumns(mTable)
                    .addOrderings(mCategory, mName, mDescription)
                    .addCondition(comboCondition)
                    .validate();

            String sql = selectQuery.toString();

            try {
                mSelectPreparedStatement = mDb.getAutoCommitConnection().prepareStatement(sql);
            } catch (SQLException ex) {
                Exceptions.printStackTrace(ex);
            }
        }

        ArrayList<MBookmark> bookmarks = new ArrayList<>();

        try {
            mStoredFilter = filter;
            filter = getFilterPattern(filter);
            mSelectPlaceHolders.get(mCategory).setString(filter, mSelectPreparedStatement);
            mSelectPlaceHolders.get(mName).setString(filter, mSelectPreparedStatement);
            mSelectPlaceHolders.get(mDescription).setString(filter, mSelectPreparedStatement);

            ResultSet rs = mSelectPreparedStatement.executeQuery();
            rs.beforeFirst();

            while (rs.next()) {
                MBookmark bookmark = new MBookmark();
                bookmark.setId(getLong(rs, mId));
                bookmark.setName(getString(rs, mName));
                bookmark.setCategory(getString(rs, mCategory));
                bookmark.setDescription(getString(rs, mDescription));
                bookmark.setColor(getString(rs, mColor));
                bookmark.setDisplayMarker(getBoolean(rs, mDisplayMarker));
                bookmark.setLatitude(getDouble(rs, mLatitude));
                bookmark.setLongitude(getDouble(rs, mLongitude));
                bookmark.setZoom(getDouble(rs, mZoom));
                bookmark.setTimeCreated(getTimestamp(rs, mTimeCreated));
                bookmark.setTimeModified(getTimestamp(rs, mTimeModified));

                bookmarks.add(bookmark);
            }
            if (addToList) {
                getItems().setAll(bookmarks);
            }
        } catch (SQLException ex) {
            Exceptions.printStackTrace(ex);
        }

        //debugPrint();
        return bookmarks;
    }

    public synchronized void dbTruncate() throws ClassNotFoundException, SQLException {
        mDb.truncate(mTable);
        dbLoad();
    }

    public void editBookmark(final MBookmark aBookmark) {
        SwingUtilities.invokeLater(() -> {
            MBookmark newBookmark = aBookmark;
            boolean add = aBookmark == null;
            if (add) {
                newBookmark = new MBookmark();
                newBookmark.setZoom(Mapton.getEngine().getZoom());
                newBookmark.setLatitude(Mapton.getEngine().getLockedLatitude());
                newBookmark.setLongitude(Mapton.getEngine().getLockedLongitude());
            }

            final MBookmark bookmark = newBookmark;
            BookmarkPanel bookmarkPanel = new BookmarkPanel();
            DialogDescriptor d = new DialogDescriptor(bookmarkPanel, Dict.BOOKMARK.toString());
            bookmarkPanel.setDialogDescriptor(d);
            bookmarkPanel.initFx(() -> {
                bookmarkPanel.load(bookmark);
            });

            bookmarkPanel.setPreferredSize(new Dimension(300, 500));
            if (DialogDescriptor.OK_OPTION == DialogDisplayer.getDefault().notify(d)) {
                bookmarkPanel.save(bookmark);
                Platform.runLater(() -> {
                    try {
                        if (add) {
                            dbInsert(bookmark);
                        } else {
                            bookmark.setTimeModified(new Timestamp(System.currentTimeMillis()));
                            dbUpdate(bookmark);
                            dbLoad();
                        }
                    } catch (ClassNotFoundException | SQLException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                });
            }
        });
    }

    public void editCategory(final String category) {
        SwingUtilities.invokeLater(() -> {
            BookmarkCategoryPanel bookmarkCategoryPanel = new BookmarkCategoryPanel();
            DialogDescriptor d = new DialogDescriptor(bookmarkCategoryPanel, Dict.EDIT.toString());
            bookmarkCategoryPanel.setDialogDescriptor(d);
            bookmarkCategoryPanel.initFx(() -> {
                bookmarkCategoryPanel.setCategory(category);
            });

            bookmarkCategoryPanel.setPreferredSize(new Dimension(400, 100));

            if (DialogDescriptor.OK_OPTION == DialogDisplayer.getDefault().notify(d)) {
                String newCategory = bookmarkCategoryPanel.getCategory();
                if (!StringUtils.equals(category, newCategory)) {
                    Timestamp timestamp = new Timestamp(System.currentTimeMillis());

                    TreeSet<String> bookmarkNames = new TreeSet<>();
                    for (MBookmark bookmark : mItems.get()) {
                        if (StringUtils.startsWith(bookmark.getCategory(), category)) {
                            String oldCategory = bookmark.getCategory();
                            bookmark.setCategory(StringUtils.replaceOnce(bookmark.getCategory(), category, newCategory));
                            bookmark.setTimeModified(timestamp);
                            try {
                                dbUpdate(bookmark);
                            } catch (SQLException ex) {
                                bookmarkNames.add(String.format("%s/%s", oldCategory, bookmark.getName()));
                            }
                        }
                    }

                    Platform.runLater(() -> {
                        dbLoad();
                    });

                    if (!bookmarkNames.isEmpty()) {
                        String delim = "\n ◆ ";
                        NbMessage.error(Dict.Dialog.ERROR.toString(),
                                String.format("%s\n%s%s", mBundle.getString("bookmark_rename_category_error"), delim, String.join(delim, bookmarkNames))
                        );
                    }
                }
            }
        });
    }

    public void editColor(final String category) {
        SwingUtilities.invokeLater(() -> {
            BookmarkColorPanel bookmarkColorPanel = new BookmarkColorPanel();
            DialogDescriptor d = new DialogDescriptor(bookmarkColorPanel, Dict.EDIT.toString());
            bookmarkColorPanel.setDialogDescriptor(d);
            bookmarkColorPanel.initFx(() -> {
            });

            bookmarkColorPanel.setPreferredSize(new Dimension(200, 100));
            if (DialogDescriptor.OK_OPTION == DialogDisplayer.getDefault().notify(d)) {
                String color = bookmarkColorPanel.getColor();
                Timestamp timestamp = new Timestamp(System.currentTimeMillis());

                for (MBookmark bookmark : mItems.get()) {
                    if (StringUtils.startsWith(bookmark.getCategory(), category)) {
                        bookmark.setColor(color);
                        bookmark.setTimeModified(timestamp);
                        try {
                            dbUpdate(bookmark);
                        } catch (SQLException ex) {
                            Exceptions.printStackTrace(ex);
                        }
                    }
                }

                Platform.runLater(() -> {
                    dbLoad();
                });
            }
        });

    }

    public void editZoom(final String category) {
        SwingUtilities.invokeLater(() -> {
            BookmarkZoomPanel bookmarkZoomPanel = new BookmarkZoomPanel();
            DialogDescriptor d = new DialogDescriptor(bookmarkZoomPanel, Dict.EDIT.toString());
            bookmarkZoomPanel.setDialogDescriptor(d);
            bookmarkZoomPanel.initFx(() -> {
            });

            bookmarkZoomPanel.setPreferredSize(new Dimension(200, 100));
            if (DialogDescriptor.OK_OPTION == DialogDisplayer.getDefault().notify(d)) {
                double zoom = bookmarkZoomPanel.getZoom();
                Timestamp timestamp = new Timestamp(System.currentTimeMillis());

                for (MBookmark bookmark : mItems.get()) {
                    if (StringUtils.startsWith(bookmark.getCategory(), category)) {
                        bookmark.setZoom(zoom);
                        bookmark.setTimeModified(timestamp);
                        try {
                            dbUpdate(bookmark);
                        } catch (SQLException ex) {
                            Exceptions.printStackTrace(ex);
                        }
                    }
                }

                Platform.runLater(() -> {
                    dbLoad();
                });
            }
        });
    }

    public boolean exists(Object exceptForValue, String name, String category) {
        HashMap<DbColumn, Object> map = new HashMap<>();
        map.put(mName, name);
        map.put(mCategory, category);

        return exists(mId, exceptForValue, map);
    }

    public FxActionSwing getAddBookmarkAction() {
        FxActionSwing action = new FxActionSwing(Dict.ADD_BOOKMARK.toString(), () -> {
            editBookmark(null);
        });

        return action;
    }

    public TreeSet<String> getCategories() {
        SelectQuery selectQuery = new SelectQuery()
                .addFromTable(mTable)
                .addColumns(mCategory)
                .addOrdering(mCategory, OrderObject.Dir.ASCENDING)
                .addGroupings(mCategory)
                .validate();

        TreeSet<String> categories = new TreeSet<>();
        categories.add("");
        try (Statement statement = mDb.getAutoCommitConnection().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
            String sql = selectQuery.toString();
            ResultSet rs = statement.executeQuery(sql);
            rs.first();
            while (rs.next()) {
                categories.add(getString(rs, mCategory));
            }

        } catch (SQLException ex) {
            //Exceptions.printStackTrace(ex);
        }

        return categories;
    }

    public MLatLonBox getExtents(String category) {
        ArrayList<MLatLon> latLons = new ArrayList<>();

        mItems.get().stream()
                .filter((bookmark) -> (StringUtils.startsWith(bookmark.getCategory(), category)))
                .forEachOrdered((bookmark) -> {
                    latLons.add(new MLatLon(bookmark.getLatitude(), bookmark.getLongitude()));
                });

        return new MLatLonBox(latLons);
    }

    public final ObservableList<MBookmark> getItems() {
        return mItems == null ? null : mItems.get();
    }

    public void goTo(MBookmark bookmark) throws ClassNotFoundException, SQLException {
        Almond.requestActive("MapTopComponent");
        Mapton.getEngine().panTo(new MLatLon(bookmark.getLatitude(), bookmark.getLongitude()), bookmark.getZoom());
    }

    public final ObjectProperty<ObservableList<MBookmark>> itemsProperty() {
        if (mItems == null) {
            mItems = new SimpleObjectProperty<>(this, "items");
        }

        return mItems;
    }

    private void addMissingColumns() {
        try {
            mDb.addMissingColumn(mTable.getAbsoluteName(), COL_COLOR, SQL_VARCHAR + "(10)", COL_DESCRIPTION);
        } catch (SQLException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private void dbInsertSilent(MBookmark bookmark) throws ClassNotFoundException, SQLException {
        if (mInsertPreparedStatement == null) {
            mInsertPlaceHolders.init(
                    mName,
                    mCategory,
                    mDescription,
                    mColor,
                    mDisplayMarker,
                    mLatitude,
                    mLongitude,
                    mZoom,
                    mTimeCreated
            );

            InsertQuery insertQuery = new InsertQuery(mTable)
                    .addColumn(mName, mInsertPlaceHolders.get(mName))
                    .addColumn(mCategory, mInsertPlaceHolders.get(mCategory))
                    .addColumn(mDescription, mInsertPlaceHolders.get(mDescription))
                    .addColumn(mColor, mInsertPlaceHolders.get(mColor))
                    .addColumn(mDisplayMarker, mInsertPlaceHolders.get(mDisplayMarker))
                    .addColumn(mLatitude, mInsertPlaceHolders.get(mLatitude))
                    .addColumn(mLongitude, mInsertPlaceHolders.get(mLongitude))
                    .addColumn(mZoom, mInsertPlaceHolders.get(mZoom))
                    .addColumn(mTimeCreated, mInsertPlaceHolders.get(mTimeCreated))
                    .validate();

            String sql = insertQuery.toString();
            mInsertPreparedStatement = mDb.getAutoCommitConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            //System.out.println(mInsertPreparedStatement.toString());
        }

        mInsertPlaceHolders.get(mName).setString(bookmark.getName(), mInsertPreparedStatement);
        mInsertPlaceHolders.get(mCategory).setString(bookmark.getCategory(), mInsertPreparedStatement);
        mInsertPlaceHolders.get(mDescription).setString(bookmark.getDescription(), mInsertPreparedStatement);
        mInsertPlaceHolders.get(mColor).setString(bookmark.getColor(), mInsertPreparedStatement);
        mInsertPlaceHolders.get(mDisplayMarker).setBoolean(bookmark.isDisplayMarker(), mInsertPreparedStatement);
        mInsertPlaceHolders.get(mLatitude).setObject(bookmark.getLatitude(), mInsertPreparedStatement);
        mInsertPlaceHolders.get(mLongitude).setObject(bookmark.getLongitude(), mInsertPreparedStatement);
        mInsertPlaceHolders.get(mZoom).setObject(bookmark.getZoom(), mInsertPreparedStatement);
        mInsertPlaceHolders.get(mTimeCreated).setObject(new Timestamp(System.currentTimeMillis()), mInsertPreparedStatement);

        int affectedRows = mInsertPreparedStatement.executeUpdate();
        if (affectedRows == 0) {
            Exceptions.printStackTrace(new SQLException("Creating bookmark failed"));
        }

    }

    private void dbUpdate(MBookmark bookmark) throws SQLException {
        if (mUpdatePreparedStatement == null) {
            mUpdatePlaceHolders.init(
                    mId,
                    mName,
                    mCategory,
                    mDescription,
                    mColor,
                    mDisplayMarker,
                    mLatitude,
                    mLongitude,
                    mZoom,
                    mTimeModified
            );

            UpdateQuery updateQuery = new UpdateQuery(mTable)
                    .addCondition(BinaryCondition.equalTo(mId, mUpdatePlaceHolders.get(mId)))
                    .addSetClause(mName, mUpdatePlaceHolders.get(mName))
                    .addSetClause(mCategory, mUpdatePlaceHolders.get(mCategory))
                    .addSetClause(mDescription, mUpdatePlaceHolders.get(mDescription))
                    .addSetClause(mColor, mUpdatePlaceHolders.get(mColor))
                    .addSetClause(mDisplayMarker, mUpdatePlaceHolders.get(mDisplayMarker))
                    .addSetClause(mLatitude, mUpdatePlaceHolders.get(mLatitude))
                    .addSetClause(mLongitude, mUpdatePlaceHolders.get(mLongitude))
                    .addSetClause(mZoom, mUpdatePlaceHolders.get(mZoom))
                    .addSetClause(mTimeModified, mUpdatePlaceHolders.get(mTimeModified))
                    .validate();

            String sql = updateQuery.toString();
            mUpdatePreparedStatement = mDb.getAutoCommitConnection().prepareStatement(sql);
        }

        mUpdatePlaceHolders.get(mId).setLong(bookmark.getId(), mUpdatePreparedStatement);
        mUpdatePlaceHolders.get(mName).setString(bookmark.getName(), mUpdatePreparedStatement);
        mUpdatePlaceHolders.get(mCategory).setString(bookmark.getCategory(), mUpdatePreparedStatement);
        mUpdatePlaceHolders.get(mDescription).setString(bookmark.getDescription(), mUpdatePreparedStatement);
        mUpdatePlaceHolders.get(mColor).setString(bookmark.getColor(), mUpdatePreparedStatement);
        mUpdatePlaceHolders.get(mDisplayMarker).setBoolean(bookmark.isDisplayMarker(), mUpdatePreparedStatement);
        mUpdatePlaceHolders.get(mLatitude).setObject(bookmark.getLatitude(), mUpdatePreparedStatement);
        mUpdatePlaceHolders.get(mLongitude).setObject(bookmark.getLongitude(), mUpdatePreparedStatement);
        mUpdatePlaceHolders.get(mZoom).setObject(bookmark.getZoom(), mUpdatePreparedStatement);

        mUpdatePreparedStatement.setTimestamp(mUpdatePlaceHolders.get(mTimeModified).getIndex(), bookmark.getTimeModified());

        mUpdatePreparedStatement.executeUpdate();
    }

    private void debugPrint() {
        System.out.println("debugPrint");
        for (MBookmark bookmark : getItems()) {
            System.out.println(ToStringBuilder.reflectionToString(bookmark, ToStringStyle.MULTI_LINE_STYLE));
        }
    }

    private static class Holder {

        private static final MBookmarkManager INSTANCE = new MBookmarkManager();
    }
}
