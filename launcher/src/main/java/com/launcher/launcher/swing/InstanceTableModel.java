/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */

package com.launcher.launcher.swing;

import com.launcher.launcher.Instance;
import com.launcher.launcher.InstanceList;
import com.launcher.launcher.FancyLauncher;
import com.launcher.launcher.util.SharedLocale;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;

public class InstanceTableModel extends AbstractTableModel {

    private final InstanceList instances;
    private final Icon instanceIcon;
    private final Icon customInstanceIcon;
    private final Icon downloadIcon;

    public InstanceTableModel(InstanceList instances) {
        this.instances = instances;
        instanceIcon = SwingHelper.createIcon(FancyLauncher.class, "instance_icon.png", 16, 16);
        customInstanceIcon = SwingHelper.createIcon(FancyLauncher.class, "custom_instance_icon.png", 16, 16);
        downloadIcon = SwingHelper.createIcon(FancyLauncher.class, "download_icon.png", 14, 14);
    }

    public void update() {
        instances.sort();
        fireTableDataChanged();
    }

    @Override
    public String getColumnName(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return "";
            case 1:
                return SharedLocale.tr("launcher.modpackColumn");
            default:
                return null;
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return ImageIcon.class;
            case 1:
                return String.class;
            default:
                return null;
        }
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        switch (columnIndex) {
            case 0:
                instances.get(rowIndex).setSelected((boolean) (Boolean) value);
                break;
            case 1:
            default:
                break;
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        switch (columnIndex) {
            case 0:
                return false;
            case 1:
                return false;
            default:
                return false;
        }
    }

    @Override
    public int getRowCount() {
        return instances.size();
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Instance instance;
        switch (columnIndex) {
            case 0:
                instance = instances.get(rowIndex);
				String waMain = "WorldAutomation.Net";
				String compare = instance.getName().toLowerCase();
                if (!instance.isLocal()) {
                    return downloadIcon;
                } else if (compare.contains(waMain.toLowerCase())) {
					//JOptionPane.showMessageDialog(null, instance.getName());
                    return customInstanceIcon;
					
                } else {
					//JOptionPane.showMessageDialog(null, instance.getName());
                    return instanceIcon;
                }
            case 1:
                instance = instances.get(rowIndex);
                return instance.getTitle();
            default:
                return null;
        }
    }

}
