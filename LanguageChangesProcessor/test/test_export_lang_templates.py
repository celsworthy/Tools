# coding=utf-8
import xlwt
import unittest
import tempfile
import os

import export_lang_templates

def write_xls(file_name, sheet_name, headings, data):
    book = xlwt.Workbook()
    sheet = book.add_sheet(sheet_name)
    rowx = 0
    for colx, value in enumerate(headings):
        sheet.write(rowx, colx, value)
        sheet.set_panes_frozen(True) # frozen headings instead of split panes
        sheet.set_horz_split_pos(rowx+1) # in general, freeze after last heading row
        sheet.set_remove_splits(True) # if user does unfreeze, don't leave a split there
    for row in data:
        rowx += 1
        for colx, value in enumerate(row):
            sheet.write(rowx, colx, value)
    book.save(file_name)


def make_xls_data(path):
    hdngs = ['Hash', 'English', 'Translation']
    kinds = 'text text text'.split()
    data = [
        ["10", "A", "Z"],
        ["20", "B", "Y"]]
    write_xls(path, 'Demo', hdngs, data)


def make_lang_data_1(): 
    hnd, path = tempfile.mkstemp()
    os.close(hnd)

    data = """application.title=Robox® AutoMaker™
aboutPanel.applicationNamePart1=Auto
aboutPanel.applicationNamePart2=Maker
"""
    f = file(path, "rw+")
    f.write(data)
    f.close()
    return path


def make_lang_data_2():
    hnd, path = tempfile.mkstemp()
    os.close(hnd)

    data = """application.title=Robox2® AutoMaker™
aboutPanel.applicationNamePart1=Auto
aboutPanel.applicationNamePart2=Maker
dialogs.mess=Message
"""
    f = file(path, "rw+")
    f.write(data)
    f.close()
    return path


class TestTemplates(unittest.TestCase):


    def testGetRowsFromLanguageFile(self):
        path = make_lang_data_1()
        rows = export_lang_templates.getRowsFromLanguageFile(path)
        print rows
        self.assertEquals(3, len(rows))

    def testGetLanguageFilesDelta(self):
        path1 = make_lang_data_1()
        path2 = make_lang_data_2()

        rows = export_lang_templates.getLanguageFilesDelta(path1, path2)
        print rows
        self.assertEquals(2, len(rows))
        deltaKeys = set([row.key for row in rows.values()])
        self.assertEquals(set(["application.title", "dialogs.mess"]), deltaKeys)

    def testGetRowsFromXLS(self):
        hnd, path = tempfile.mkstemp()
        os.close(hnd)
        make_xls_data(path)
        rows = export_lang_templates.getRowsFromXLS(path)
        print rows
        self.assertEquals(3, len(rows))

    def testMakeTemplateFileFromDeltaRows(self):
        path1 = make_lang_data_1()
        path2 = make_lang_data_2()

        deltaRowsByHash = export_lang_templates.getLanguageFilesDelta(path1, path2)
        hnd, pathTemplateXLS = tempfile.mkstemp()
        os.close(hnd)
        #deltaTemplateXLS = makeTemplateFileFromDeltaRows(deltaRows, pathTemplateXLS, languageCode)
        deltaTemplateXLS = export_lang_templates.makeTemplateFileFromDeltaRows(
            deltaRowsByHash.values(), "/tmp/DEOUT.xls", "DE")
        
