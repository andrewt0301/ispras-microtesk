#
# Copyright 2018 ISP RAS (http://www.ispras.ru)
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
from minimips_base import MiniMipsBaseTemplate

class EuclidTemplate(MiniMipsBaseTemplate):
    def __init__(self):
        MiniMipsBaseTemplate.__init__(self)
        
    def run(self):
            self.add(self.t0(), self.zero(), self.zero())
            self.addi(self.t1(), self.zero(), 99)
            self.add(self.t2(), self.zero(), self.zero())


template = EuclidTemplate()
template.generate()
        