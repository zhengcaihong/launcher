find $1 -regex ".*\.xml" | xargs sed -i "/@aui_color_test_open@/d"
