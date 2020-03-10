package main

import (
	"net"
	"io/ioutil"
	"fmt"
)

var (
	host string = "3.88.183.122"
	flag []byte
	buff []byte
	request []byte
	payload []byte
	fragSize int = 1024
)

var checksum []uint16 = []uint16{0xe4c7, 0xe4c6, 0xe4c5, 0xe4c4, 0xe4c3,
0xe4c2, 0xe4c1, 0xe4c0, 0xe4bf, 0xe4be, 0xe4bd, 0xe4bc, 0xe4bb, 0xe4ba, 0xe4b9,
0xe4b8}

func check(e error) {
	if e != nil {
		panic(e)
	}
}


func main() {
	// Setup Buffers
	flag = make([]byte, 16384)
	payload = make([]byte, 8 + 48)

	// Open Connection
	conn, err := net.Dial("ip4:icmp", host)
	check(err)

	// Build base payload
	request = []byte{8,0,0,0,0x13,0x37, 0, 0}
	copy(payload[:], request)
	for i := 1; i <= 16; i++ {
		buff = make([]byte, 1052)
		fmt.Printf("Requesting %d\n", i)
		// Adding Sequence number
		payload[6] = uint8(uint16(i)>>8)
		payload[7] = uint8(uint16(i)&0xff)


		// Calculating and Adding Checksum
		csum := checksum[i-1]
		payload[2] = uint8(csum >> 8)
		payload[3] = uint8(csum & 0xff)

		// Send request
		_, err := conn.Write(payload)
		check(err)

		// Read Response
		total := 0
		for total != 1052  {
			n, err := conn.Read(buff[total:])
			check(err)
			total += n
		}
		start := (i-1)*fragSize
		end := start + fragSize
		copy(flag[start:end], buff[28:])
	}

	err = ioutil.WriteFile("flag", flag, 0666)
	check(err)
}
