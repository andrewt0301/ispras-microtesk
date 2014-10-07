#
# Copyright (c) 2014 ISPRAS (www.ispras.ru)
#
# Institute for System Programming of Russian Academy of Sciences
#
# 25 Alexander Solzhenitsyn st. Moscow 109004 Russia
#
# All rights reserved.
#
# datagen.rb, Oct 3, 2014 1:34:30 PM Andrei Tatarnikov
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

require ENV['TEMPLATE']

#
# Description:
#
# This test template demonstrates the use of test situations and 
# data generators in MicroTESK.
#
# Possible syntax styles:
#
# Style 1:
#
# vliw(
#   (addi r(4), r(0), 5  do situation('overflow') end),
#   (addi r(5), r(0), 10 do situation('normal') end)
# )
#
# Style 2:
#
# vliw(
#   addi(r(4), r(0), 5)  do situation('overflow') end,
#   addi(r(5), r(0), 10) do situation('normal') end
# )
#
class VLIWDemo < Template

  def initialize
    super
    @is_executable = true
  end

  def run
    trace 'Data Generation Example: Debug Output'

    # Random immediate values: rand(min, max)
    comment 'rand(0, 31)'
    vliw(
      (addi r(rand(1, 31)), r(0), rand(0, 31)),
      (addi r(rand(1, 31)), r(0), rand(0, 31))
    )

    # Another way to generate random immediate values:
    # situation imm_random(min, max).
    comment 'imm_random (:min => 1, :max => 31)'
    vliw(
      (addi r(_), r(_), _ do situation('imm_random', :min => 1, :max => 31) end),
      (addi r(_), r(_), _ do situation('imm_random', :min => 1, :max => 31) end)
    )

    # Or this way:
    comment 'imm_random (:min => 1, :max => 31) - all together'
    vliw(
      (addi r(_), r(_), _),
      (addi r(_), r(_), _)
    ) do situation('imm_random', :min => 1, :max => 31) end

    # Immediate values are produced as an incremental sequence in
    # the specified range with the specified step {1, 3, 5, 7, ... }
    comment 'imm_range (:from => 1, :to => 31, :step => 2)'
    vliw(
      (add r(_), r(_), r(_)),
      (add r(_), r(_), r(_))
    ) do situation('imm_range', :from => 1, :to => 31, :step => 2) end

    # All registers are filled with zeros.
    comment 'zero (:size => 32)'
    vliw(
      (add r(1), r(3), r(5)),
      (add r(2), r(4), r(6))
    ) do situation('zero', :size => 32) end

    # Random registers are filled with random values.
    comment 'random (:size => 32, :min_imm => 1, :max_imm => 31)'
    vliw(
      (add r(_), r(_), r(_)),
      (add r(_), r(_), r(_))
    ) do situation('random', :size => 32, :min_imm => 1, :max_imm => 31) end

    vliw(
      (addi r(4), r(0), 5  do situation('overflow') end),
      (addi r(5), r(0), 10 do situation('normal') end)
    )

    vliw(
      (add r(6), r(1), r(8) do situation('overflow', :x => 1, :y => 'test') end),
      (add r(7), r(2), r(9) do situation('normal') end)
    )
    
    vliw(
      (addi r(4), r(0), 5  do situation('overflow') end),
      nop
    )

    vliw(
      (add r(6), r(1), r(8) do situation('overflow') end),
      nop
    )


  end

  def gpr(index)
    location('GPR', index)
  end

end
