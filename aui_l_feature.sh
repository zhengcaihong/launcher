find $1 -regex ".*\.xml" | xargs sed -i "/@aui_l_feature_open@/d"
