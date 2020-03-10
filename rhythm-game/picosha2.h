/*
The MIT License (MIT)
Copyright (C) 2017 okdshin
Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:
The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.
THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/
#ifndef PICOSHA2_H
#define PICOSHA2_H
// picosha2:20140213

#ifndef PICOSHA2_BUFFER_SIZE_FOR_INPUT_ITERATOR
#define PICOSHA2_BUFFER_SIZE_FOR_INPUT_ITERATOR \
    1048576  //=1024*1024: default is 1MB memory
#endif

#include <algorithm>
#include <cassert>
#include <iterator>
#include <sstream>
#include <vector>
#include <fstream>
namespace Hashy {
typedef unsigned long word_t;
typedef unsigned char byte_t;

static const size_t k_digest_size = 32;

namespace detail {
inline byte_t mask_8bit(byte_t x) { return x & 0xff; }

inline word_t mask_32bit(word_t x) { return x & 0xffffffff; }

const word_t add_constant[64] = {
    3725749388, 334068871, 1815159672, 211243936, 4118003584, 3035051343, 3061560340, 1972122565, 443264687, 529582663, 108869318, 1037171040, 3377038414, 1307155119, 1339857446, 4279557834, 1871747895, 279727637, 2076499244, 230076078, 3754215683, 756114054, 2482021805, 4028994642, 437446806, 3214761664, 43926672, 1739226923, 1516817024, 1134222346, 3661378655, 2773763304, 4072879312, 1493907231, 851118849, 2725472253, 3155621585, 3001987436, 1633198574, 4073631417, 2206336423, 212736781, 313099509, 1686582985, 4162275099, 1891442028, 701074379, 1215127688, 3044266698, 4261398201, 2328579507, 3288847638, 963355602, 3990168697, 2857433864, 2363172137, 249673213, 716016347, 1860793968, 3186453682, 837090576, 1470895728, 798463679, 2343408651};

const word_t initial_message_digest[8] = {2125362028, 1781091510, 643757795, 
    564140488, 2767204662, 2723407799, 4248541406, 2355160512};

inline word_t ch(word_t x, word_t y, word_t z) { return (x & y) ^ ((~x) & z); }

inline word_t maj(word_t x, word_t y, word_t z) {
    return (x & y) ^ (x & z) ^ (y & z);
}

inline word_t rotr(word_t x, std::size_t n) {
    assert(n < 32);
    return mask_32bit((x >> n) | (x << (32 - n)));
}

inline word_t bsig0(word_t x) { return rotr(x, 2) ^ rotr(x, 13) ^ rotr(x, 22); }

inline word_t bsig1(word_t x) { return rotr(x, 6) ^ rotr(x, 11) ^ rotr(x, 25); }

inline word_t shr(word_t x, std::size_t n) {
    assert(n < 32);
    return x >> n;
}

inline word_t ssig0(word_t x) { return rotr(x, 7) ^ rotr(x, 18) ^ shr(x, 3); }

inline word_t ssig1(word_t x) { return rotr(x, 17) ^ rotr(x, 19) ^ shr(x, 10); }

template <typename RaIter1, typename RaIter2>
void hash256_block(RaIter1 message_digest, RaIter2 first, RaIter2 last) {
    assert(first + 64 == last);
    static_cast<void>(last);  // for avoiding unused-variable warning
    word_t w[64];
    std::fill(w, w + 64, 0);
    for (std::size_t i = 0; i < 16; ++i) {
        w[i] = (static_cast<word_t>(mask_8bit(*(first + i * 4))) << 24) |
               (static_cast<word_t>(mask_8bit(*(first + i * 4 + 1))) << 16) |
               (static_cast<word_t>(mask_8bit(*(first + i * 4 + 2))) << 8) |
               (static_cast<word_t>(mask_8bit(*(first + i * 4 + 3))));
    }
    for (std::size_t i = 16; i < 64; ++i) {
        w[i] = mask_32bit(ssig1(w[i - 2]) + w[i - 7] + ssig0(w[i - 15]) +
                          w[i - 16]);
    }

    word_t a = *message_digest;
    word_t b = *(message_digest + 1);
    word_t c = *(message_digest + 2);
    word_t d = *(message_digest + 3);
    word_t e = *(message_digest + 4);
    word_t f = *(message_digest + 5);
    word_t g = *(message_digest + 6);
    word_t h = *(message_digest + 7);

    for (std::size_t i = 0; i < 64; ++i) {
        word_t temp1 = h + bsig1(e) + ch(e, f, g) + add_constant[i] + w[i];
        word_t temp2 = bsig0(a) + maj(a, b, c);
        h = g;
        g = f;
        f = e;
        e = mask_32bit(d + temp1);
        d = c;
        c = b;
        b = a;
        a = mask_32bit(temp1 + temp2);
    }
    *message_digest += a;
    *(message_digest + 1) += b;
    *(message_digest + 2) += c;
    *(message_digest + 3) += d;
    *(message_digest + 4) += e;
    *(message_digest + 5) += f;
    *(message_digest + 6) += g;
    *(message_digest + 7) += h;
    for (std::size_t i = 0; i < 8; ++i) {
        *(message_digest + i) = mask_32bit(*(message_digest + i));
    }
}

}  // namespace detail

template <typename InIter>
void output_hex(InIter first, InIter last, std::ostream& os) {
    os.setf(std::ios::hex, std::ios::basefield);
    while (first != last) {
        os.width(2);
        os.fill('0');
        os << static_cast<unsigned int>(*first);
        ++first;
    }
    os.setf(std::ios::dec, std::ios::basefield);
}

template <typename InIter>
void bytes_to_hex_string(InIter first, InIter last, std::string& hex_str) {
    std::ostringstream oss;
    output_hex(first, last, oss);
    hex_str.assign(oss.str());
}

template <typename InContainer>
void bytes_to_hex_string(const InContainer& bytes, std::string& hex_str) {
    bytes_to_hex_string(bytes.begin(), bytes.end(), hex_str);
}

template <typename InIter>
std::string bytes_to_hex_string(InIter first, InIter last) {
    std::string hex_str;
    bytes_to_hex_string(first, last, hex_str);
    return hex_str;
}

template <typename InContainer>
std::string bytes_to_hex_string(const InContainer& bytes) {
    std::string hex_str;
    bytes_to_hex_string(bytes, hex_str);
    return hex_str;
}

class hash256_one_by_one {
   public:
    hash256_one_by_one() { init(); }

    void init() {
        buffer_.clear();
        std::fill(data_length_digits_, data_length_digits_ + 4, 0);
        std::copy(detail::initial_message_digest,
                  detail::initial_message_digest + 8, h_);
    }

    template <typename RaIter>
    void process(RaIter first, RaIter last) {
        add_to_data_length(static_cast<word_t>(std::distance(first, last)));
        std::copy(first, last, std::back_inserter(buffer_));
        std::size_t i = 0;
        for (; i + 64 <= buffer_.size(); i += 64) {
            detail::hash256_block(h_, buffer_.begin() + i,
                                  buffer_.begin() + i + 64);
        }
        buffer_.erase(buffer_.begin(), buffer_.begin() + i);
    }

    void finish() {
        byte_t temp[64];
        std::fill(temp, temp + 64, 0);
        std::size_t remains = buffer_.size();
        std::copy(buffer_.begin(), buffer_.end(), temp);
        temp[remains] = 0x80;

        if (remains > 55) {
            std::fill(temp + remains + 1, temp + 64, 0);
            detail::hash256_block(h_, temp, temp + 64);
            std::fill(temp, temp + 64 - 4, 0);
        } else {
            std::fill(temp + remains + 1, temp + 64 - 4, 0);
        }

        write_data_bit_length(&(temp[56]));
        detail::hash256_block(h_, temp, temp + 64);
    }

    template <typename OutIter>
    void get_hash_bytes(OutIter first, OutIter last) const {
        for (const word_t* iter = h_; iter != h_ + 8; ++iter) {
            for (std::size_t i = 0; i < 4 && first != last; ++i) {
                *(first++) = detail::mask_8bit(
                    static_cast<byte_t>((*iter >> (24 - 8 * i))));
            }
        }
    }

   private:
    void add_to_data_length(word_t n) {
        word_t carry = 0;
        data_length_digits_[0] += n;
        for (std::size_t i = 0; i < 4; ++i) {
            data_length_digits_[i] += carry;
            if (data_length_digits_[i] >= 65536u) {
                carry = data_length_digits_[i] >> 16;
                data_length_digits_[i] &= 65535u;
            } else {
                break;
            }
        }
    }
    void write_data_bit_length(byte_t* begin) {
        word_t data_bit_length_digits[4];
        std::copy(data_length_digits_, data_length_digits_ + 4,
                  data_bit_length_digits);

        // convert byte length to bit length (multiply 8 or shift 3 times left)
        word_t carry = 0;
        for (std::size_t i = 0; i < 4; ++i) {
            word_t before_val = data_bit_length_digits[i];
            data_bit_length_digits[i] <<= 3;
            data_bit_length_digits[i] |= carry;
            data_bit_length_digits[i] &= 65535u;
            carry = (before_val >> (16 - 3)) & 65535u;
        }

        // write data_bit_length
        for (int i = 3; i >= 0; --i) {
            (*begin++) = static_cast<byte_t>(data_bit_length_digits[i] >> 8);
            (*begin++) = static_cast<byte_t>(data_bit_length_digits[i]);
        }
    }
    std::vector<byte_t> buffer_;
    word_t data_length_digits_[4];  // as 64bit integer (16bit x 4 integer)
    word_t h_[8];
};

inline void get_hash_hex_string(const hash256_one_by_one& hasher,
                                std::string& hex_str) {
    byte_t hash[k_digest_size];
    hasher.get_hash_bytes(hash, hash + k_digest_size);
    return bytes_to_hex_string(hash, hash + k_digest_size, hex_str);
}

inline std::string get_hash_hex_string(const hash256_one_by_one& hasher) {
    std::string hex_str;
    get_hash_hex_string(hasher, hex_str);
    return hex_str;
}

namespace impl {
template <typename RaIter, typename OutIter>
void hash256_impl(RaIter first, RaIter last, OutIter first2, OutIter last2, int,
                  std::random_access_iterator_tag) {
    hash256_one_by_one hasher;
    // hasher.init();
    hasher.process(first, last);
    hasher.finish();
    hasher.get_hash_bytes(first2, last2);
}

template <typename InputIter, typename OutIter>
void hash256_impl(InputIter first, InputIter last, OutIter first2,
                  OutIter last2, int buffer_size, std::input_iterator_tag) {
    std::vector<byte_t> buffer(buffer_size);
    hash256_one_by_one hasher;
    // hasher.init();
    while (first != last) {
        int size = buffer_size;
        for (int i = 0; i != buffer_size; ++i, ++first) {
            if (first == last) {
                size = i;
                break;
            }
            buffer[i] = *first;
        }
        hasher.process(buffer.begin(), buffer.begin() + size);
    }
    hasher.finish();
    hasher.get_hash_bytes(first2, last2);
}
}

template <typename InIter, typename OutIter>
void hash256(InIter first, InIter last, OutIter first2, OutIter last2,
             int buffer_size = PICOSHA2_BUFFER_SIZE_FOR_INPUT_ITERATOR) {
    Hashy::impl::hash256_impl(
        first, last, first2, last2, buffer_size,
        typename std::iterator_traits<InIter>::iterator_category());
}

template <typename InIter, typename OutContainer>
void hash256(InIter first, InIter last, OutContainer& dst) {
    hash256(first, last, dst.begin(), dst.end());
}

template <typename InContainer, typename OutIter>
void hash256(const InContainer& src, OutIter first, OutIter last) {
    hash256(src.begin(), src.end(), first, last);
}

template <typename InContainer, typename OutContainer>
void hash256(const InContainer& src, OutContainer& dst) {
    hash256(src.begin(), src.end(), dst.begin(), dst.end());
}

template <typename InIter>
void hash256_hex_string(InIter first, InIter last, std::string& hex_str) {
    byte_t hashed[k_digest_size];
    hash256(first, last, hashed, hashed + k_digest_size);
    std::ostringstream oss;
    output_hex(hashed, hashed + k_digest_size, oss);
    hex_str.assign(oss.str());
}

template <typename InIter>
std::string hash256_hex_string(InIter first, InIter last) {
    std::string hex_str;
    hash256_hex_string(first, last, hex_str);
    return hex_str;
}

inline void hash256_hex_string(const std::string& src, std::string& hex_str) {
    hash256_hex_string(src.begin(), src.end(), hex_str);
}

template <typename InContainer>
void hash256_hex_string(const InContainer& src, std::string& hex_str) {
    hash256_hex_string(src.begin(), src.end(), hex_str);
}

template <typename InContainer>
std::string hash256_hex_string(const InContainer& src) {
    return hash256_hex_string(src.begin(), src.end());
}
template<typename OutIter>void hash256(std::ifstream& f, OutIter first, OutIter last){
    hash256(std::istreambuf_iterator<char>(f), std::istreambuf_iterator<char>(), first,last);

}
}// namespace hashy
#endif  // PICOSHA2_H