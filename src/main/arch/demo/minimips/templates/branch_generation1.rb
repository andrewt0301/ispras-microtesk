#
# Copyright 2017 ISP RAS (http://www.ispras.ru)
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

require_relative 'minimips_base'

#
# Description:
#
# This test template demonstrates how to generate test cases with branch instructions
# including instruction with two parameters such as beq and bne. Currently, there is 
# a limitation: only one parameter is stored and loaded from the stream. Consequently,
# the second parameter must be a register with a predefined value such as $zero.
#
class BranchGeneration1Template < MiniMipsBaseTemplate

  def initialize
    super
    set_option_value 'default-test-data', false
  end

  def pre
    super

    data {
      org 0x00010000
      align 8
      # Arrays to store test data for branch instructions.
      label :branch_data_0
      word 0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,
           0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,
           0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,
           0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,
           0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,
           0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,
           0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,
           0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0
      label :branch_data_1
      word 0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,
           0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,
           0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,
           0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,
           0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,
           0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,
           0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,
           0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0
      label :branch_data_2
      word 0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,
           0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,
           0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,
           0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,
           0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,
           0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,
           0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,
           0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0
      label :branch_data_3
      word 0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,
           0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,
           0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,
           0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,
           0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,
           0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,
           0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,
           0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0,    0x0, 0x0, 0x0, 0x0
    }

    stream_preparator(:data_source => 'REG', :index_source => 'REG') {
      init {
        la index_source, start_label
      }

      read {
        lw data_source, 0x0, index_source
        addiu index_source, index_source, 4
      }

      write {
        sw data_source, 0x0, index_source
        addiu index_source, index_source, 4
      }
    }
  end

  def run
    org 0x00020000

    # Stream  Label            Data  Addr  Size
    stream   :branch_data_0,   s0,   s4,   128
    stream   :branch_data_1,   s1,   s5,   128
    stream   :branch_data_2,   s2,   s6,   128
    stream   :branch_data_3,   s3,   s7,   128

    # A branch structure is as follows:
    #
    #  0: NOP
    #  1: if (BGTZ) then goto 0
    #  2: NOP
    #  3: if (BLTZ) then goto 2
    #  4: NOP
    #  5: if (BEQ) then goto 4
    #  6: NOP
    #  7: if (BNE) then goto 10
    #  8: NOP
    #  9: goto 0
    # 10: NOP

    # Parameter 'branch_exec_limit' bounds the number of executions of a single branch:
    #   the default value is 1.
    # Parameter 'trace_count_limit' bounds the number of execution traces to be created:
    #   the default value is -1 (no limitation).
    sequence(
          :engines => {
              :branch => {:branch_exec_limit => 3,
                          :block_exec_limit => 3,
                          :trace_count_limit => -1}}) {
      label :label0
        nop
        bgtz s0, :label0 do
          situation('bgtz-if-then', :engine => :branch, :stream => 'branch_data_0')
        end
        addiu reg1=get_register, reg1, 1

      label :label1
        nop
        bltz s1, :label1 do
          situation('bltz-if-then', :engine => :branch, :stream => 'branch_data_1')
        end
        ori reg2=get_register, reg2, 2

      label :label2
        nop
        beq s2, zero, :label2 do
          situation('beq-if-then', :engine => :branch, :stream => 'branch_data_2')
        end
        addiu reg3=get_register, reg3, 3

      label :label3
        nop
        bne s3, zero, :label5 do
          situation('bne-if-then', :engine => :branch, :stream => 'branch_data_3')
        end
        ori reg4=get_register, reg4, 4

      label :label4
        nop
        j :label0 do
          situation('b-goto', :engine => :branch)
        end
        addiu reg5=get_register, reg5, 5

      label :label5
        nop
    }.run
  end
end
