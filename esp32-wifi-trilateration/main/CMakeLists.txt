idf_component_register(
    SRCS "ap_list.c" "strings.c" "trilateration.c" "esp32-wifi-positioning.c"
    PRIV_REQUIRES esp_wifi nvs_flash
    INCLUDE_DIRS "."
)
