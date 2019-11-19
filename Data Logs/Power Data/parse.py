def main():
    fr = open("GPS_ON_SCREEN_ON.txt", "r")
    fw = open("wattage_ON_ON.txt", "a+")
    read = fr.readlines()
    for i in read:
        if i.__contains__(","):
            sep = i.split(",")
            watts = float(sep[0]) * float(sep[1])
            fw.write(str(watts) + "\n")
        else:
            fw.write("TEST\n")
    fr.close()
    fw.close()

if __name__ == "__main__":
    main()