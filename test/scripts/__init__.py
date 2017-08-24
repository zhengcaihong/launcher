#! /usr/bin/python
# -*- coding: utf8 -*-


'''
Created on 2014-9-23

@author: weidu.yjy
'''
#the name for test cases' binary
TAR_FOLDER_NAME = r"ts_homeshell.tar"
#the name for product binary package
TAR_BINARY_NAME = r"kfzc_binary_homeshell.tar"
#the tar name for response time test
TEST_RESPONE_TIME_JAR_NAME = ""
#the name for generated test case jar
TEST_JAR_NAME = "homeshell_ui_testcase.jar"
#will copy the sub folders / files under res from test/src/[TEST_RES_PARENT_FOLDER]/res
TEST_RES_PARENT_FOLDER = ".."
#will copy the testcase folders / files under testcase from test/src/[TEST_TC_PARENT_FOLDER]/res
TEST_TC_PARENT_FOLDER = ".."
#the default lab id in kelude for running test cases.
LAB_ID = ""
#the default device needed for running automation tests
DEVICE_TAG = "homeshell-KFZC"
#the OS version will be used to determine which
OS_VER = "3.0"
