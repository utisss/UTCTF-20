package main

import (
	"net"
)

var (
	host string = "0.0.0.0"
	hosts map[string][]byte = map[string][]byte{
		"dns.google.com": []byte{35, 188, 185, 68},
		"flag": []byte("haha"),
	}
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

	answer, in := hosts[cmdS]
	if !in {
		answer = []byte("nope")
	}

	queryLen := ptr + 5
	ansLen := len(answer)
	resp := make([]byte, queryLen)
	copy(resp[:12], []byte{buff[0], buff[1], 0x81, 0x80, 0x00, 0x01, 0x00, 0x01,
	0x00, 0x00, 0x00, 0x00})
	copy(resp[12:queryLen], buff[12:])

	answerB := []byte{0xc0, 0x0c, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00, 0x13, 0x37,
	0x00, byte(ansLen)}
	answerB = append(answerB, []byte(answer)...)
	resp = append(resp, answerB...)

	_, err := conn.WriteToUDP(resp, addr)
	if err != nil {
		return
	}
}
